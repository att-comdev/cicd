
// Required credentials names
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh


// Jenkins global env variables (: examples)
//  - JENKINS_URL
//  - JENKINS_CLI


def openstack_cmd(String cmd, String mount = "") {

    docker_env = " -e OS_AUTH_URL=${OS_AUTH_URL}" +
                 " -e OS_PROJECT_DOMAIN_NAME=default" +
                 " -e OS_USER_DOMAIN_NAME=default" +
                 " -e OS_PROJECT_NAME=${OS_PROJECT_NAME}" +
                 " -e OS_REGION_NAME=${OS_REGION_NAME}" +
                 " -e OS_USERNAME=\$OS_USERNAME" +
                 " -e OS_PASSWORD=\$OS_PASSWORD" +
                 " -e OS_IDENTITY_API_VERSION=3"

    docker_opts = "--rm --net=host"

    if (mount) {
        docker_opts += " -v ${mount}:/target"
    }

    return "sudo docker run ${docker_opts} ${docker_env} ${OS_KEYSTONE_IMAGE} ${cmd}"
}

//parameters example:  --parameter 'image=${imageName}' --parameter 'flavor=m1.xlarge'
def stack_create(String name, String tmpl, String parameters) {
    heat.create("${name}", "${tmpl}", "--parameter 'flavor=m1.xlarge'")
}


def stack_delete(String name) {
    heat.delete("${name}", "${tmpl}")
}


def stack_ip_get(String name) {
    heat.stack_output(${name}, "floating_ip")
}

//jenkins-slave-ssh is already in use for the foundry.  We need to standardize to something not in use.
def jenkins_node_config(String name, String host) {
    config = """<slave>
        <name>${name}</name>
        <description></description>
        <remoteFS>/home/ubuntu/jenkins</remoteFS>
        <numExecutors>2</numExecutors>
        <mode>EXCLUSIVE</mode>
        <retentionStrategy class=\"hudson.slaves.RetentionStrategy\$Always\"/>
        <launcher class=\"hudson.plugins.sshslaves.SSHLauncher\" plugin=\"ssh-slaves@1.5\">
        <host>${host}</host>
        <port>22</port>
        <credentialsId>jenkins-ssh-slave</credentialsId>
        <maxNumRetries>0</maxNumRetries>
        <retryWaitTime>0</retryWaitTime>
        </launcher>
        <label>${name}</label>
        <nodeProperties/>
        <sshHostKeyVerificationStrategy class="hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy"/>
        <userId>ubuntu</userId>
        </slave>"""
    return config
}

/**
 * Crate single node VM from heat template/user-data
 *
 * @param nodeTemplate Heat template relative to resources/heat
 * @param userData Bootstrap script for the VM
 * @param vmPostfix Additional postfix to identify the VM
**/
def call(nodeTemplate, userData, imageName, vmPostfix = '', keepRunning = false, Closure body) {

    // node used for launching VMs
    def launch_node = 'jenkins-node-launch'

    def name = "${JOB_BASE_NAME}-${BUILD_NUMBER}"

    // optionally uer may supply additional identified for the VM
    // this makes it easier to find it in OpenStack
    if (vmPostfix) {
      name += "-${vmPostfix}"
    }

    def ip = ""

    try {
        stage ('Node Launch') {
            node(launch_node) {
                tmpl = libraryResource "heat/nova/${nodeTemplate}"
                writeFile file: 'template.yaml', text: tmpl

                udata = libraryResource "heat/${userData}"
                writeFile file: 'bootstrap.sh', text: udata

                stack_create(name, "${WORKSPACE}/template.yaml", imageName)
                ip = stack_ip_get(name)
            }

            node('master') {
              jenkins_node_create (name, ip)
            }

            node(launch_node) {
                 timeout (14) {
                    node(name) {
                        sh 'hostname'
                    }
                }
            }
        }

        // body executed under specified vm node
        node (name) {
            body()
        }

    } catch (error) {
        notify.msg("Pipeline failed: ${error}")
        error(error)

    } finally {
        try {
            node(name){
                stage("Publish Jenkins Logs"){
                    try{
                        publish.putArtifacts(logs.getJenkinsConsoleOutput(), "logs/${JOB_NAME}/")
                    } catch (error){
                        notify.msg("Logs published failed: ${error}")
                    }
                }
            }
        } finally {
            stage ('Node Destroy') {
                node(launch_node) {
                    try {
                        jenkins_vm_destroy(name)
                    } catch (error) {
                        notify.msg("Node destroy failed: ${error}")
                    }
                }
            }
        }
    }
  return ip
}
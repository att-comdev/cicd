
// Required credentials names
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh


// Jenkins global env variables (: examples)
//  - JENKINS_URL
//  - JENKINS_CLI


def openstack_cmd(String cmd, String mount = "") {
    keystone_image = "kolla/ubuntu-source-keystone:3.0.3"

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

    return "docker run ${docker_opts} ${docker_env} ${keystone_image} ${cmd}"
}


def stack_create(String name, String tmpl) {


    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                          usernameVariable: 'OS_USERNAME',
                                          passwordVariable: 'OS_PASSWORD')]) {

        cmd = openstack_cmd("openstack stack create -t /target/\$(basename ${tmpl}) ${name}", "\$(dirname ${tmpl})")
        code = sh (script: cmd, returnStatus: true)
        if (!code) {
            // todo: improve timeouts to more user friendly look
            timeout = 300
            for (i = 0; i < timeout; i=i+10) {
                sleep 30
                cmd = openstack_cmd("openstack stack show -f value -c stack_status ${name}")
                ret = sh (script: cmd, returnStdout: true).trim()
                if (ret == "CREATE_COMPLETE") {
                    print "Stack ${name} created!"
                    return
                } else if (ret != "CREATE_IN_PROGRESS") {
                    print "Failed to create stack ${name}"
                    sh "exit 1"
                }
            }
        }
        print "Failed to create stack ${name}"
        sh "exit 1"
    }
}


def stack_delete(String name) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                          usernameVariable: 'OS_USERNAME',
                                          passwordVariable: 'OS_PASSWORD')]) {

        cmd = openstack_cmd("openstack stack delete --yes $name")
        code = sh (script: cmd, returnStatus: true)
        if (!code) {
            timeout = 30
            for (i = 0; i < timeout; i=i+5) {
                sleep 10
                cmd = openstack_cmd("openstack stack list")
                ret = sh (script: cmd, returnStdout: true)
                if (!ret.contains(name)) {
                    print "Stack ${name} deleted!"
                    return
                }
            }
            print "Failed to delete stack ${name}"
            sh "exit 1" //
        } else {
            print "Likely stack ${name} did not exist! It's OK."
        }
    }
}


def stack_ip_get(String name) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                      usernameVariable: 'OS_USERNAME',
                                      passwordVariable: 'OS_PASSWORD')]) {
        cmd = openstack_cmd("openstack stack output show -f value -c output_value ${name} floating_ip")
        return sh(returnStdout: true, script: cmd).trim()
    }
}


def jenkins_node_config(String name, String host) {
    config = """<slave>
        <name>${name}</name>
        <description></description>
        <remoteFS>/home/ubuntu/jenkins</remoteFS>
        <numExecutors>4</numExecutors>
        <mode>EXCLUSIVE</mode>
        <retentionStrategy class=\"hudson.slaves.RetentionStrategy\$Always\"/>
        <launcher class=\"hudson.plugins.sshslaves.SSHLauncher\" plugin=\"ssh-slaves@1.5\">
        <host>${host}</host>
        <port>22</port>
        <credentialsId>jenkins</credentialsId>
        </launcher>
        <label>${name}</label>
        <nodeProperties/>
        <userId>ubuntu</userId>
        </slave>"""
    return config
}


def jenkins_node_create(String name, String host) {
    config = jenkins_node_config(name, host)
    withCredentials([usernamePassword(credentialsId: 'jenkins-token',
                                      usernameVariable: 'JENKINS_USER',
                                      passwordVariable: 'JENKINS_TOKEN')]) {

        opts = "-s \$JENKINS_CLI_URL -auth \$JENKINS_USER:\$JENKINS_TOKEN"
        cmd = "echo '${config}' | java -jar \$JENKINS_CLI ${opts} create-node ${name}"
        sh (script: cmd, returnStatus: true)
    }
}


def jenkins_node_delete(String name) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-token',
                                      usernameVariable: 'JENKINS_USER',
                                      passwordVariable: 'JENKINS_TOKEN')]) {

        opts = "-s \$JENKINS_URL -auth \$JENKINS_USER:\$JENKINS_TOKEN"
        cmd = "java -jar \$JENKINS_CLI $opts delete-node $name"
        code = sh (script: cmd , returnStatus: true)
        // todo: handle exit code properly
    }
}


/**
 * Crate single node VM from heat template/user-data
 *
 * @param nodeTemplate Heat template relative to resources/heat
 * @param userData Bootstrap script for the VM
**/
def call(nodeTemplate, userData, Closure body) {

    // node used for launching vms
    def launch_node = 'local-vm-launch'

    // I believe this is generic enough for VM names
    def name = "${JOB_BASE_NAME}-${BUILD_NUMBER}"
    def ip = ""

    try {
        stage ('Node Launch') {
            node(launch_node) {
                tmpl = libraryResource "heat/${nodeTemplate}"
                writeFile file: 'template.yaml', text: tmpl

                udata = libraryResource "heat/${userData}"
                writeFile file: 'bootstrap.sh', text: udata

                stack_create(name, "${WORKSPACE}/template.yaml")
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
        stage ('Node Destroy') {
            node(launch_node) {
                jenkins_node_delete(name)
            }
            node('master') {
                stack_delete(name)
            }
        }
    }
}


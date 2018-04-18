
// Required credentials names
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh


// Jenkins global env variables (: examples)
//  - JENKINS_URL
//  - JENKINS_CLI

//parameters example:  --parameter 'image=${imageName}' --parameter 'flavor=m1.xlarge'
def stack_create(String name, String tmpl, String parameters) {
  heat.stack_create("${name}", "${tmpl}", "${parameters}")
}


def stack_delete(String name) {
    heat.stack_delete("${name}", "${tmpl}")
}


def stack_ip_get(String name) {
    heat.stack_output(${name}, "floating_ip")
}

/**
 * Crate single node VM from heat template/user-data
 *
 * @param nodeTemplate Heat template relative to resources/heat
 * @param userData Bootstrap script for the VM
 * @param vmPostfix Additional postfix to identify the VM
**/
def call(nodeTemplate,
         userData,
         image = 'cicd-ubuntu-16.04-server-cloudimg-amd64',
         vmPostfix = '',
         keepRunning = false,
         Closure body) {

    def parameters = "--parameter bootstrap=${userData}--parameter image=${image}"
  
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

                stack_create(name, "${WORKSPACE}/template.yaml", parameters)
                ip = stack_ip_get(name)
            }

            node('master') {
                jenkins.node_create (name, ip)
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

// Required credentials names
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh


// Jenkins global env variables (: examples)
//  - JENKINS_URL
//  - JENKINS_CLI

/**
 * Create single node using a heat template
 * Usage:
 *       vm2([:]) - All defaults
 *       vm2(leak:true)
 *       vm2(initScript:'loci-bootstrap.sh', buildType:'loci')
 *       vm2(flavor:'m1.xlarge', image:'cicd-ubuntu-18.04-server-cloudimg-amd64', initScript:'loci-bootstrap.sh')
 *
**/
def call(Map map,
         Closure body) {
    def initScript = map.initScript ?: 'bootstrap.sh'
    def image = map.image ?: 'cicd-ubuntu-16.04-server-cloudimg-amd64'
    def flavor = map.flavor ?: 'm1.medium'
    def nodePostfix = map.nodePostfix ?: ''
    def buildType = map.buildType ?: 'basic'
    def leak = map.leak ?: false

    // resolve args to heat parameters
    def parameters = " --parameter image=${image}" +
                     " --parameter flavor=${flavor}"

    // node used for launching VMs
    def launch_node = 'jenkins-node-launch'

    def name = "${JOB_BASE_NAME}-${BUILD_NUMBER}"

    def stack_template="heat/stack/ubuntu.${buildType}.stack.template.yaml"

    // optionally uer may supply additional identified for the VM
    // this makes it easier to find it in OpenStack
    if (nodePostfix) {
      name += "-${nodePostfix}"
    }

    def ip = ""

    try {
        stage ('Node Launch') {
            node(launch_node) {
                tmpl = libraryResource "${stack_template}"
                writeFile file: 'template.yaml', text: tmpl

                data = libraryResource "heat/stack/${initScript}"
                writeFile file: initScript, text: data

                heat.stack_create(name, "${WORKSPACE}/template.yaml", parameters)
                ip = heat.stack_output(name, 'floating_ip')
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
        // notify.msg("Pipeline failed: ${error}")
        error(error)

    } finally {
        try {
            // node(name){
            //     stage("Publish Jenkins Logs"){
            //         try{
            //             publish.putArtifacts(logs.getJenkinsConsoleOutput(), "logs/${JOB_NAME}/")
            //         } catch (error){
            //             notify.msg("Logs published failed: ${error}")
            //         }
            //     }
            // }
        } finally {
            if (!leak) {
                stage ('Node Destroy') {
                    node('master') {
                        jenkins.node_delete(name)
                    }
                    node(launch_node) {
                       heat.stack_delete(name)
                    }
                }
            }
        }
    }
  return ip
}

/**
 *  This method allow for conventional usage as vm2()
 *       vm2() - All defaults
 *
**/
def call(Closure body) {
   map = [:]
    call(map, body)
}

def setproxy(){
    if (HTTP_PROXY){
        sh'''sudo mkdir -p /etc/systemd/system/docker.service.d
             cat <<-EOF | sudo tee -a /etc/systemd/system/docker.service.d/http-proxy.conf
             [Service]
             Environment="HTTP_PROXY=${HTTP_PROXY}"
             Environment="HTTPS_PROXY=${HTTP_PROXY}"
             Environment="NO_PROXY=127.0.0.1,localhost"
             EOF'''
        sh'''cat <<-EOF | sudo tee -a /etc/environment
             http_proxy=${HTTP_PROXY}
             https_proxy=${HTTP_PROXY}
             EOF'''
        sh "sudo systemctl daemon-reload"
        sh "sudo systemctl restart docker"
        sh 'export http_proxy=${HTTP_PROXY}'
        sh 'export https_proxy=${HTTP_PROXY}'
    }
}

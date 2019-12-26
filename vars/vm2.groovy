
// Required credentials names
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh


// Jenkins global env variables (: examples)
//  - JENKINS_URL
//  - JENKINS_CLI


/**
 * Create single node using a heat template
**/
def call(udata = 'bootstrap.sh',
         image = 'cicd-ubuntu-16.04-server-cloudimg-amd64',
         flavor = 'm1.medium',
         postfix = '',
         buildtype = 'basic',
         leak = false,
         numOfExecutors = 2,
         Closure body) {

    // resolve args to heat parameters
    def parameters = " --parameter image=${image}" +
                     " --parameter flavor=${flavor}"

    // node used for launching VMs
    def launch_node = 'jenkins-node-launch'

    def name = "${JOB_BASE_NAME}-${BUILD_NUMBER}"

    def stack_template="heat/stack/ubuntu.${buildtype}.stack.template.yaml"

    if (env.OS_JUMPHOST_PUBLIC_IP) {
        error ("'vm2' does not support jumphost. Please switch to 'vm'.")
    }

    // optionally uer may supply additional identified for the VM
    // this makes it easier to find it in OpenStack
    if (postfix) {
      name += "-${postfix}"
    }

    def ip = ""

    try {
        stage ('Node Launch') {
            node(launch_node) {
                tmpl = libraryResource "${stack_template}"
                writeFile file: 'template.yaml', text: tmpl

                data = libraryResource "heat/stack/${udata}"
                writeFile file: 'cloud-config', text: data

                heat.stack_create(name, "${WORKSPACE}/template.yaml", parameters)
                ip = heat.stack_output(name, 'floating_ip')
            }

            node('master') {
                jenkins.node_create (name, ip, '22', 'jenkins-slave-ssh', numOfExecutors)
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
            if (env.VM_PRE_HOOK_CMD) {
                sh VM_PRE_HOOK_CMD
            }
            setKnownHosts()
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
 * This method is used for any Jenkins pipelines that are behind the proxy.  They are currently
 * set to global variables in Jenkins, if you have no firewall you can define these parameters as
 * empty string in globals.
 *
 */
def setproxy(){
    if (HTTP_PROXY){

        // redirection with "<<-" doesnot work well to remove whitespaces/tabs
        sh'''sudo mkdir -p /etc/systemd/system/docker.service.d
             cat << EOF | sudo tee -a /etc/systemd/system/docker.service.d/http-proxy.conf
[Service]
Environment="HTTP_PROXY=${HTTP_PROXY}"
Environment="HTTPS_PROXY=${HTTP_PROXY}"
Environment="NO_PROXY=${NO_PROXY}"
EOF'''

        sh'''cat << EOF | sudo tee -a /etc/environment
http_proxy=${HTTP_PROXY}
https_proxy=${HTTP_PROXY}
no_proxy=${NO_PROXY}
HTTP_PROXY=${HTTP_PROXY}
HTTPS_PROXY=${HTTP_PROXY}
NO_PROXY=${NO_PROXY}
EOF'''

        sh "sudo systemctl daemon-reload"
        sh "sudo systemctl restart docker"
        sh 'export http_proxy=${HTTP_PROXY}'
        sh 'export https_proxy=${HTTP_PROXY}'
        sh 'export no_proxy=${NO_PROXY}'
        sh 'export HTTP_PROXY=${HTTP_PROXY}'
        sh 'export HTTPS_PROXY=${HTTP_PROXY}'
        sh 'export NO_PROXY=${NO_PROXY}'
    }
}

/**
 * This method updates known_hosts for each slave from Jenkins variable
 * to prevent minm attacks. ssh-keyscan needs to be removed and KNOWN_HOSTS to be
 * populated with proper keys.
 */
def setKnownHosts() {
    if (env.KNOWN_HOSTS) {
        sh "mkdir -p ${HOME}/.ssh; echo \"${KNOWN_HOSTS}\" >> ${HOME}/.ssh/known_hosts"
    }
}


// Required Jenkins credentials
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh


// Jenkins global env variables
//  - JENKINS_URL
//  - JENKINS_CLI
//  - ARTF_WEB_URL


/**
 *
 */
def message(String headline, Closure body) {
     print '======================================================================'
     print headline
     print '======================================================================'
     body()
     print '----------------------------------------------------------------------'
}


/**
 * Create single node using a heat template
 * Usage:
 *       vm([:]) - All defaults
 *       vm(doNotDeleteNode:true)
 *       vm(initScript:'loci-bootstrap.sh', buildType:'loci')
 *       vm(flavor:'m1.xlarge',
 *           image:'cicd-ubuntu-18.04-server-cloudimg-amd64',
 *           initScript:'loci-bootstrap.sh')
 *
**/
def call(Map map, Closure body) {

    // Startup script to run after VM instance creation
    //  bootstrap.sh - default
    //  loci-bootstrap.sh - for loci builds
    def initScript = map.initScript ?: 'bootstrap.sh'

    // image used for creating instance
    def image = map.image ?: 'cicd-ubuntu-16.04-server-cloudimg-amd64'

    // flavor type used for creating instance
    def flavor = map.flavor ?: 'm1.medium'

    // postfix string for instance nodename
    def nodePostfix = map.nodePostfix ?: ''

    // build template used for heat stack creation
    //  basic - default
    //  loci - for loci builds
    def buildType = map.buildType ?: 'basic'

    // Flag to control node cleanup after job execution
    // Useful for retaining env for debugging failures
    // NodeCleanup job be used to destroy the node later
    //  false - default, deletes node after job
    //  true - do not delete node
    def doNotDeleteNode = map.doNotDeleteNode ?: false

    // Flag to control Jenkins console log publishing to Artifactory.
    //
    // This will also set custom URL to be returned when voting in Gerrit
    // https://jenkins.io/doc/pipeline/steps/gerrit-trigger/
    //
    // Useful for providing Jenkins console log when acting as 3rd party gate,
    // especially when Jenkins itself is not accessible
    def artifactoryLogs = map.artifactoryLogs ?: false

    // global timeout for executing pipeline
    // useful to prevent forever hanging pipelines consuming resources
    def globalTimeout = map.timeout ?: 120

    // if useJumphost is true floating ip won't be assigned to vm.
    // Jenkins will access vm via jumphost configured in global configuration
    // with OS_JUMPHOST_PUBLIC_IP variable
    def useJumphost = map.useJumphost
    if (useJumphost == null) {
        useJumphost = env.OS_JUMPHOST_PUBLIC_IP ? true : false
    }

    // Name of public network that is used to allocate floating IPs
    def publicNet = useJumphost ? '' : (map.publicNet ?:
                                        env.OS_PUBLIC_NET ?:
                                        'public')

    // Name of private network for the VM
    def privateNet = map.privateNet ?: 'private'

    // resolve args to heat parameters
    def parameters = " --parameter image=${image}" +
                     " --parameter flavor=${flavor}" +
                     " --parameter public_net=${publicNet}" +
                     " --parameter private_net=${privateNet}"

    // node used for launching VMs
    def launch_node = 'jenkins-node-launch'

    def name = "${JOB_BASE_NAME}-${BUILD_NUMBER}"

    // templates located in resources from shared libraries
    // https://github.com/att-comdev/cicd/tree/master/resources
    def stack_template="heat/stack/ubuntu.${buildType}.stack.template.yaml"

    // optionally uer may supply additional identified for the VM
    // this makes it easier to find it in OpenStack (e.g. name)
    if (nodePostfix) {
      name += "-${nodePostfix}"
    }

    def ip = ""
    def port = "22"

    try {
        stage ('Node Launch') {

            node(launch_node) {
                tmpl = libraryResource "${stack_template}"
                writeFile file: 'template.yaml', text: tmpl

                data = libraryResource "heat/stack/${initScript}"
                writeFile file: 'cloud-config', text: data

                heat.stack_create(name, "${WORKSPACE}/template.yaml", parameters)
                ip = heat.stack_output(name, 'floating_ip')
                if (useJumphost) {
                    port = (ip.split('\\.')[-1].toInteger() + 10000).toString()
                    ip = OS_JUMPHOST_PUBLIC_IP
                }
            }

            node('master') {
                jenkins.node_create (name, ip, port)

                timeout (14) {
                    node(name) {
                        sh 'cloud-init status --wait'
                    }
                }
            }
        }

        // execute pipeline body, everything within vm()
        node (name) {
            try {
                message ('READY: JENKINS WORKER LAUNCHED') {
                    print "Launch overrides: ${map}\n" +
                          "Pipeline timeout: ${globalTimeout}\n" +
                          "Heat template: ${stack_template}\n" +
                          "Node IP: ${ip}:${port}"
                }
                if (env.VM_PRE_HOOK_CMD) {
                    sh VM_PRE_HOOK_CMD
                }
                timeout(globalTimeout) {
                    setKnownHosts()
                    body()
                }
                message ('SUCCESS: PIPELINE EXECUTION FINISHED') {}
                currentBuild.result = 'SUCCESS'

              // use Throwable to catch java.lang.NoSuchMethodError error
            } catch (Throwable err) {
                message ('FAILURE: PIPELINE EXECUTION HALTED') {
                    print "Pipeline body failed or timed out: ${err}.\n" +
                          'Likely gate reports failure.\n'
                }
                currentBuild.result = 'FAILURE'
                throw err
            }
        }

      // use Throwable to catch java.lang.NoSuchMethodError error
    } catch (Throwable err) {
        message ('ERROR: FAILED TO LAUNCH JENKINS WORKER') {
            print 'Failed to launch Jenkins VM/worker.\n' +
                  'Likely infra/template or config error.\n' +
                  "Error message: ${err}"
        }
        currentBuild.result = 'FAILURE'
        if (env.GERRIT_EVENT_TYPE == "change-merged"){
            email.sendMail(recipientProviders: [developers(), requestor()],
                           to: env.EMAIL_LIST)
        }
        throw err

    } finally {
        if (!doNotDeleteNode) {
            node('master') {
                jenkins.node_delete(name)
            }
            node(launch_node) {
               heat.stack_delete(name)
            }
        }

        // publish Jenkins console output
        // note: keep this as very last step to capture most logs
        if (artifactoryLogs) {
            node('master'){
                message ('INFO: PUBLISHING LOGS TO ARTIFACTORY') {}
                try{
                    def logBase = "cicd/logs/${JOB_NAME}/${BUILD_ID}"

                    sh "curl -s -o console.txt ${BUILD_URL}/consoleText"
                    artifactory.upload ('console.txt', "${logBase}/console.txt")

                    // Jenkins global variable for Artifactory URL
                    setGerritReview customUrl: "${ARTF_WEB_URL}/${logBase}"

                } catch (error){
                    // gracefully handle failures to publish
                    print "Failed to publish logs to Artifactory: ${err}"
                }
            }
        }

    }
  return [ip, port]
}


/**
 *  This method allow for conventional usage as vm()
 *       vm() - All defaults
**/
def call(Closure body) {
   map = [:]
    call(map, body)
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

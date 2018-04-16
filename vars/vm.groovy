
// Required credentials names
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh


// Jenkins global env variables (: examples)
//  - JENKINS_URL
//  - JENKINS_CLI


def openstack_cmd(String cmd, String mount = "") {
    keystone_image = "kolla/ubuntu-source-keystone:3.0.3"

    docker_env = " -e OS_AUTH_URL=http://keystone/v3" +
                 " -e OS_PROJECT_DOMAIN_NAME=default" +
                 " -e OS_USER_DOMAIN_NAME=default" +
                 " -e OS_PROJECT_NAME=service" +
                 " -e OS_REGION_NAME=RegionOne" +
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
        //This will wait until the VM comes up and will error if it hasn't in 15 minutes
        cmd = openstack_cmd("openstack stack create --wait --timeout 15 -t /target/\$(basename ${tmpl}) ${name}", "\$(dirname ${tmpl})")
        code = sh (script: cmd, returnStatus: true)

        cmd = openstack_cmd("openstack stack output show ${name} ip -f value -c output_value")
        code = sh (script: cmd, returnStatus: true)
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

        opts = "-s \$JENKINS_URL -auth \$JENKINS_USER:\$JENKINS_TOKEN"
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


def jenkins_vm_launch(String name, String tmpl, String host = "") {

    stack_create(name, tmpl)

    // use floating ip if not specified
    if (!host) {
        host = stack_ip_get(name)
    }
    jenkins_node_create (name, host)
}


def jenkins_vm_destroy(String name) {
    jenkins_node_delete(name)
    stack_delete(name)
}



// single node/vm job template
//  - create vm based on gven template
//  - clean-up after exceptions/failures
//  - timeout if node is not getting ready

def call(name, tmpl, Closure body) {

    // node used for launching vms
    def launch_node = 'local-vm-launch'

    try {
        stage ('Node Launch') {
            node(launch_node) {
                jenkins_vm_launch(name, "${HOME}/${tmpl}")

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
        node(name){
            stage("Publish Jenkins Logs"){
                try{
                    if("x${GERRIT_CHANGE_NUMBER}" != "x" && "x${GERRIT_PATCHSET_NUMBER}" != "x" && "x${BUILD_URL}" != "x") {
                        sh "curl -s -o ./${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}.log ${BUILD_URL}consoleText"
                        publish.uploadArtifacts("./${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}.log", "logs")
                    } else if("x${BUILD_URL}" != "x" && "x${JOB_BASE_NAME}" != "x" && "x${BUILD_NUMBER}" != "x") {
                        sh "curl -s -o ./${JOB_BASE_NAME}-${BUILD_NUMBER}.log ${BUILD_URL}consoleText"
                        publish.uploadArtifacts("./${JOB_BASE_NAME}-${BUILD_NUMBER}.log", "logs")
                    } else {
                        notify.msg("No logs published, verify this is a valid Jenkins job.")
                    }
                } catch (error){
                    notify.msg("Logs published failed: ${error}")
                }
            }
        }
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
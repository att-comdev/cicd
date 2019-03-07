
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

//example of parameters --parameter 'cloudImage=${cloudImage}' --parameter ...
def stack_create(String name, String tmpl, String parameters) {

    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                          usernameVariable: 'OS_USERNAME',
                                          passwordVariable: 'OS_PASSWORD')]) {


        // note: occasionally stack may fail to create (likely OpenStack issue)
        retry (3) {
            try {
                sh openstack_cmd("openstack stack create --wait -t /target/\$(basename ${tmpl}) ${name} ${parameters}", "\$(dirname ${tmpl})")
            } catch (err) {
                print "Failed to create stack ${name} -> ${err.getMessage()}"
                stack_delete(name)
                sleep 30
                throw err
            }
        }
    }
}


def stack_delete(String name) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                          usernameVariable: 'OS_USERNAME',
                                          passwordVariable: 'OS_PASSWORD')]) {

        cmd = openstack_cmd("openstack stack list")
        ret = sh (script: cmd, returnStdout: true)
        if (!ret.contains(name)) {
            print "Stack ${name} is already deleted, skipping!"
            return
        }

        // note: occasionally stack may fail to delete (likely OpenStack issue)
        retry (3) {
            try {
                sh openstack_cmd("openstack stack delete --wait --yes ${name}")
            } catch (err) {
                print "Failed to delete stack ${name} -> ${err.getMessage()}"
                sleep 10
                throw err
            }
        }
    }
}


def stack_output(String name, String output) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                      usernameVariable: 'OS_USERNAME',
                                      passwordVariable: 'OS_PASSWORD')]) {
        cmd = openstack_cmd("openstack stack output show -f value -c output_value ${name} ${output}")
        return sh(returnStdout: true, script: cmd).trim()
    }
}

def stack_status(String name) {
    cmd = openstack_cmd("openstack stack show -f value -c stack_status ${name}")
    ret = sh (script: cmd, returnStdout: true).trim()
    if (ret != "CREATE_COMPLETE") {
        print "Failed to create stack ${name}"
        sh "exit 1"
    }
}

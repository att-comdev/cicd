// Required credentials names
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh


// Jenkins global env variables (: examples)
//  - JENKINS_URL
//  - JENKINS_CLI


def openstack_cmd(String cmd, String mount = "", Boolean useHeatContainer = true) {
    openstack_credentials = [
        'OS_AUTH_URL': OS_AUTH_URL,
        'OS_PROJECT_DOMAIN_NAME': 'default',
        'OS_USER_DOMAIN_NAME': 'default',
        'OS_PROJECT_NAME': OS_PROJECT_NAME,
        'OS_REGION_NAME': OS_REGION_NAME,
        'OS_USERNAME': OS_USERNAME,
        'OS_PASSWORD': OS_PASSWORD,
        'OS_IDENTITY_API_VERSION':'3'
    ]

    if (useHeatContainer == false) {
        return "${openstack_credentials.collect { "${it.key}=${it.value}" }.join(' ')} $cmd"
    }

    docker_env = openstack_credentials.collect { "-e ${it.key}=${it.value}" }.join(' ')
    docker_opts = "--rm --net=host"

    if (mount) {
        docker_opts += " -v ${mount}:/target"
    }

    return "sudo docker run ${docker_opts} ${docker_env} ${OS_KEYSTONE_IMAGE} ${cmd}"
}

//example of parameters --parameter 'cloudImage=${cloudImage}' --parameter ...
def stack_create(String name, String tmpl, String parameters, Boolean useHeatContainer = true) {

  withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                          usernameVariable: 'OS_USERNAME',
                                          passwordVariable: 'OS_PASSWORD')]) {
        String cmd
        if (useHeatContainer) {
            cmd = openstack_cmd("openstack stack create -t '/target/\$(basename ${tmpl})' '${name}' ${parameters} --wait", "\$(dirname ${tmpl})")
        } else {
            cmd = openstack_cmd("openstack stack create -t '${tmpl}' '${name}' ${parameters} --wait", null, false)
        }
        code = sh (script: cmd, returnStatus: true)
        if (!code) {
            // todo: improve timeouts to more user friendly look
            timeout = 300
            for (i = 0; i < timeout; i=i+10) {
                cmd = openstack_cmd("openstack stack show -f value -c stack_status ${name}", null, useHeatContainer)
                ret = sh (script: cmd, returnStdout: true).trim()
                if (ret == "CREATE_COMPLETE") {
                    print "Stack ${name} created!"
                    return
                } else if (ret != "CREATE_IN_PROGRESS") {
                    cmd = openstack_cmd("openstack stack show ${name}", null, useHeatContainer)
                    ret = sh (script: cmd, returnStdout: true)
                    print "Stack status:\n${ret}"
                    print "Heat stack error ${name}"
                    sh "exit 1"
                }
                sleep 30
            }
        }
        print "Failed to create stack ${name}"
        sh "exit 1"
    }
}


def stack_delete(String name, Boolean useHeatContainer = true) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                          usernameVariable: 'OS_USERNAME',
                                          passwordVariable: 'OS_PASSWORD')]) {
        cmd = openstack_cmd("openstack stack delete --wait --yes ${name}", null, useHeatContainer)
        code = sh (script: cmd, returnStatus: true)
        if (!code) {
            cmd = openstack_cmd("openstack stack list", null, useHeatContainer)
            ret = sh (script: cmd, returnStdout: true)
            if (!ret.contains(name)) {
                print "Stack ${name} deleted!"
                return
            }
            print "Failed to delete stack ${name}"
            sh "exit 1" //
        } else {
            print "Likely stack ${name} did not exist! It's OK."
        }
    }
}


def stack_output(String name, String output, Boolean useHeatContainer = true) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                      usernameVariable: 'OS_USERNAME',
                                      passwordVariable: 'OS_PASSWORD')]) {
        cmd = openstack_cmd("openstack stack output show -f value -c output_value ${name} ${output}", null, useHeatContainer)
        return sh(returnStdout: true, script: cmd).trim()
    }
}

def stack_status(String name, Boolean useHeatContainer = true) {
    cmd = openstack_cmd("openstack stack show -f value -c stack_status ${name}", null, useHeatContainer)
    ret = sh (script: cmd, returnStdout: true).trim()
    if (ret != "CREATE_COMPLETE") {
        print "Failed to create stack ${name}"
        sh "exit 1"
    }
}
#!groovy

// credentials
//  - jenkins-openstack
//  - jenkins-token
//  - jenkins-slave-ssh

// envs
// - JENKINS_URL
// - JENKINS_CLI


def openstack_cmd(String cmd, String mount) {
    keystone_image = "kolla/ubuntu-source-keystone:3.0.3"

    docker_env = "-e 'S_AUTH_URL=http://keystone/v3" +
                 " -e OS_PROJECT_DOMAIN_NAME=default" +
                 " -e OS_USER_DOMAIN_NAME=default" +
                 " -e OS_PROJECT_NAME=service" +
                 " -e OS_REGION_NAME=RegionOne" +
                 " -e OS_USERNAME=\$OS_USERNAME" +
                 " -e OS_PASSWORD=\$OS_PASSWORD" +
                 " -e OS_IDENTITY_API_VERSION=3"

    docker_opts = "--rm --net=host -v ${mount}:/target"

    return "sudo docker run ${docker_opts} ${docker_env} ${keystone_image} ${cmd}"
}


def stack_create(String name, String tmpl) {
    print "enter stack_create"
    cmd = openstack_cmd("openstack stack create -t /target/\$(basename ${tmpl}) ${name}", "\$(dirname ${tmpl})")

    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack',
                                          usernameVariable: 'OS_USERNAME',
                                          passwordVariable: 'OS_PASSWORD')]) {
        for (i = 0; i <3; i++) {
            // todo: handle errors cases
            code = sh (script: cmd, returnStatus: true)
            if (code == 0) { break; }
        }
    }
    // todo: wait for stack create complete before return
    // todo: implement wait for available resources
}


def stack_delete(String name) {
    cmd = openstack_cmd("openstack stack delete --yes $name")
    code = sh (script: cmd, returnStatus: true)
    // todo: handle graceful fail better
}


def stack_ip_get(String name) {
    cmd = openstack_cmd("openstack stack output show -f value -c output_value ${name} floating_ip")
    return sh(returnStdout: true, script: cmd).trim()
}


def jenkins_node_config(String name, String host) {
    config = """<slave>
        <name>${name}</name>
        <description></description>
        <remoteFS>/home/ubuntu/jenkins</remoteFS>
        <numExecutors>1</numExecutors>
        <mode>EXCLUSIVE</mode>
        <retentionStrategy class=\"hudson.slaves.RetentionStrategy\$Always\"/>
        <launcher class=\"hudson.plugins.sshslaves.SSHLauncher\" plugin=\"ssh-slaves@1.5\">
        <host>${host}</host>
        <port>22</port>
        <credentialsId>jenkins-slave-ssh</credentialsId>
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
        sh "java -jar \$JENKINS_CLI $opts node-create $name << $config"
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


def jenkins_slave_launch(String name, String tmpl, String host = "") {

    print "jenkins_slave_launch"
    stack_create(name, tmpl)

    // use floating ip if not specified
    if (!host) {
        host = stack_ip_get(name)
    }
    jenkins_node_create (name, host)
}


def jenkins_slave_destroy(String name) {
    jenkins_node_delete(name)
    stack_delete(name)
}


return this;


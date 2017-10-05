#!groovy

def test() {
    sh 'echo Hello!'
}


def test2() {
    echo "test2"
}

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


def jenkins_node_config(String name, String host, String creds) {
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
             <credentialsId>${creds}</credentialsId>
             </launcher>
             <label>${name}</label>
             <nodeProperties/>
             <userId>ubuntu</userId>
             </slave>"""
             return config
}

//
// def stack_create = { name, tmpl ->
//     cmd = os_cmd("openstack stack create -t ${templ} ${name}")
//
//     // retry a few times (request not accepated by Heat at times)
//     for (i = 0; i <3; i++) {
//         code = sh (script: cmd, returnStatus: true)
//         if (code == 0) { break; }
//     }
//
//     sleep 30 // wait before ask for ip
//     cmd = docker_cmd_wrap("openstack stack output show -f value -c output_value ${NODE_NAME} floating_ip")
//     NODE_IP=sh(returnStdout: true, script: cmd).trim()
//
//     withCredentials([usernamePassword(credentialsId: 'jenkins-token',
//         usernameVariable: 'JENKINS_USER',
//         passwordVariable: 'JENKINS_TOKEN')]) {
//
//         sh "bash create-single-node ${NODE_NAME} ${NODE_IP}"
//     }
// }
//
//
// def jenkins_node_create = { name, config
//
//     withCredentials([usernamePassword(credentialsId: 'jenkins-token',
//         usernameVariable: 'JENKINS_USER',
//         passwordVariable: 'JENKINS_TOKEN')]) {
//
//         opts = "-s \$JENKINS_URL -auth \$JENKINS_USER:\$JENKINS_TOKEN"
//         sh "java -jar \$JENKINS_CLI $opts $name << $config"
//     }
// }

return this;


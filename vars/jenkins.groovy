//This groovy file is used for Jenkins node methods.

def node_create(String name, String host, String key = 'jenkins-slave-ssh') {
    config = node_config(name, host, key)
    withCredentials([usernamePassword(credentialsId: 'jenkins-token',
                                      usernameVariable: 'JENKINS_USER',
                                      passwordVariable: 'JENKINS_TOKEN')]) {

        opts = "-s \$JENKINS_CLI_URL -auth \$JENKINS_USER:\$JENKINS_TOKEN"
        cmd = "echo '${config}' | java -jar \$JENKINS_CLI ${opts} create-node ${name}"
        sh (script: cmd, returnStatus: true)
    }
}


def node_delete(String name) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-token',
                                      usernameVariable: 'JENKINS_USER',
                                      passwordVariable: 'JENKINS_TOKEN')]) {

        opts = "-s \$JENKINS_CLI_URL -auth \$JENKINS_USER:\$JENKINS_TOKEN"
        cmd = "java -jar \$JENKINS_CLI $opts delete-node $name"
        code = sh (script: cmd , returnStatus: true)
        // todo: handle exit code properly
    }
}

//jenkins-slave-ssh is already in use for the foundry.  We need to standardize to something not in use.
def node_config(String name, String host, String key) {
    config = """<slave>
        <name>${name}</name>
        <description></description>
        <remoteFS>/home/ubuntu/jenkins</remoteFS>
        <numExecutors>2</numExecutors>
        <mode>EXCLUSIVE</mode>
        <retentionStrategy class=\"hudson.slaves.RetentionStrategy\$Always\"/>
        <launcher class=\"hudson.plugins.sshslaves.SSHLauncher\" plugin=\"ssh-slaves@1.5\">
        <host>${host}</host>
        <port>22</port>
        <credentialsId>${key}</credentialsId>
        <maxNumRetries>0</maxNumRetries>
        <retryWaitTime>0</retryWaitTime>
        </launcher>
        <label>${name}</label>
        <nodeProperties/>
        <sshHostKeyVerificationStrategy class="hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy"/>
        <userId>ubuntu</userId>
        </slave>"""
    return config
}

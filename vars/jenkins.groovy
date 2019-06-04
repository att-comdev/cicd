//This groovy file is used for Jenkins node methods.
import hudson.model.Node.Mode
import hudson.slaves.*
import jenkins.model.Jenkins
import hudson.plugins.sshslaves.SSHLauncher
import hudson.plugins.sshslaves.verifiers.*

def node_create(String name, String host, String key = 'jenkins-slave-ssh', Number numOfExecutors = 1){
   String agentHome = "/home/ubuntu/jenkins"
   String agentDesription = "Jenkins executator"
   SshHostKeyVerificationStrategy hostKeyVerificationStrategy = new NonVerifyingKeyVerificationStrategy()
   DumbSlave dumb = new DumbSlave(name,
      agentDesription,
      agentHome, 
      numOfExecutors, 
      Mode.EXCLUSIVE,
      name,
      new SSHLauncher(host, 22, SSHLauncher.lookupSystemCredentials(key), "", null, null, "", "", 60, 3, 15,hostKeyVerificationStrategy),
      RetentionStrategy.INSTANCE)
   Jenkins.instance.addNode(dumb) 
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


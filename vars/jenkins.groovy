//This groovy file is used for Jenkins node methods.
import hudson.model.Node.Mode
import hudson.slaves.*
import jenkins.model.Jenkins
import hudson.plugins.sshslaves.SSHLauncher
import hudson.plugins.sshslaves.verifiers.*

def node_create(String name, String host, String key = 'jenkins-slave-ssh', Number numOfExecutors = 1){
   String agentHome = '/home/ubuntu/jenkins'
   String agentDescription = 'Jenkins slave'
   SshHostKeyVerificationStrategy hostKeyVerificationStrategy = new NonVerifyingKeyVerificationStrategy()
   DumbSlave dumb = new DumbSlave(name,
       agentDescription,
       agentHome,
       numOfExecutors.toString(),
       Mode.EXCLUSIVE,
       name,
       new SSHLauncher(host, 22, SSHLauncher.lookupSystemCredentials(key), "", null, null, "", "", 60, 3, 15,hostKeyVerificationStrategy),
       RetentionStrategy.INSTANCE)

   Jenkins.instance.addNode(dumb)
}



def node_delete(String name) {
   Node node = Jenkins.instance.getNode(name)
   Jenkins.instance.removeNode(node)
}

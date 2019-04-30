import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;

import hudson.slaves.*
import hudson.plugins.sshslaves.verifiers.*
import hudson.plugins.sshslaves.SSHConnector
import hudson.plugins.sshslaves.SSHLauncher

def addSlave(ip, creds, name, port=22, timeout=null, retries=null, waitRetry=null) {
    def serverKeyVerificationStrategy = new NonVerifyingKeyVerificationStrategy()
    ComputerLauncher launcher = new SSHLauncher(
        ip,
        port,
        creds,
        (String)null, // jvmOptions
        (String)null, // javaPath
        (String)null, // prefixStartSlaveCmd
        (String)null, // suffixStartSlaveCmd
        (Integer) timeout, // launchTimeoutSeconds,
        (Integer) retries, //maxNumRetries
        (Integer) waitRetry, // retryWaitTime
        serverKeyVerificationStrategy // Host Key Verification Strategy
    )

    Slave agent = new DumbSlave(name, "/home/jenkins", launcher)
    agent.nodeDescription = name
    agent.numExecutors = 1
    Jenkins.instance.addNode(agent)
}



/**
 * Return a collection of Global credentials defined within Jenkins
*/
def getGlobalCreds() {
    return SystemCredentialsProvider.getInstance().getStore().getCredentials(Domain.global())
}

/**
 * Return a list of Global credential IDs defined within Jenkins
*/
def getGlobalCredIds() {
    def globalCreds = getGlobalCreds()
    def credIds = []
    for(cred in globalCreds) {
        credIds.add(cred.id.toString())
    }
    return credIds
}

/**
 * Create a Global user/password credential within Jenkins
 *
 * @param id the ID you wish the global credential to have
 * @param description the description you wish the global credential to have
 * @param user the username of the global credential you're creating
 * @param pass the password of the global credential you're creating
*/
def createGlobalCred(id, description, user, pass) {
    Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, user, pass)
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
}

/**
 * Create a Global SSH user/key credential within Jenkins
 *
 * @param id the ID you wish the global credential to have
 * @param description the description you wish the global credential to have
 * @param user the username of the global credential you're creating
 * @param key the private key of the global credential you're creating
*/
def createGlobalSshCred(id, description, user, key) {
    def privateKeySource = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(key)
    def secret = new BasicSSHUserPrivateKey(CredentialsScope.GLOBAL, id, user, privateKeySource, "", id)
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), secret)
}

/**
 * Delete a Global SSH user/key credential within Jenkins
 *
 * @param id the ID you wish the global credential to have
*/
def deleteGlobalSshCred(id) {
    def globalCreds = getGlobalCreds()
    def c = globalCreds.findResult { it.id == id ? it : null }
    if (c) {
        SystemCredentialsProvider.getInstance().getStore().removeCredentials(Domain.global(), c)
    }
}

/**
 * Delete a Global user/password credential within Jenkins
 *
 * @param id the ID of the established global credential
 * @param description the description of the established global credential
 * @param user the username of the global credential you're deleting
 * @param pass the password of the global credential you're deleting
*/
def deleteGlobalCred(id, description, user, pass) {
    Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, user, pass)
    SystemCredentialsProvider.getInstance().getStore().removeCredentials(Domain.global(), c)
}

/**
 * Recreate a Global user/password credential within Jenkins (delete -> create)
 * Note: delete should not fail if the credential does not already exist
 *
 * @param id the ID of the global credential
 * @param description the description you wish the global credential
 * @param user the username of the global credential you're deleting/creating
 * @param pass the password of the global credential you're deleting/creating
*/
def recreateGlobalCred(id, description, user, pass) {
    deleteGlobalCred(id, description, user, pass)
    createGlobalCred(id, description, user, pass)
}

/** Helper for update ssh key for jenkins slave
 *
 * @param node String Slave's name
 * @param key String New cred id
 */
def updateSshKeySlave(node, key) {
    def slave = Hudson.instance.slaves.find({it.name == node});
    def slaveLauncher = slave.getLauncher()
    slaveLauncher.credentialsId = key
}

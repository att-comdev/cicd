import jenkins.model.*;
import com.cloudbees.hudson.plugins.folder.*;
import com.cloudbees.hudson.plugins.folder.properties.*;
import hudson.plugins.sshslaves.*;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;

/**
 * Return a collection of Global credentials defined within Jenkins
*/
def getGlobalCreds() {
    return SystemCredentialsProvider.getInstance().getStore().getCredentials(Domain.global())
}

def getFolderCreds(root) {
    root.getAllItems(com.cloudbees.hudson.plugins.folder.Folder.class).each{ f ->
        creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
                com.cloudbees.plugins.credentials.Credentials.class, f)
    }
    return creds
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

def getFolderCredIds() {
    def folderCreds = getFolderCreds()
    def credsIds = []
    for (cred in folderCreds) {
        credsIds.add(cred.id.toString())
    }
    return credsIds
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

def createFolderCred(id, description, user, pass) {
    Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, user, pass)
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.deploys(), c)
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
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.deploys(), secret)
}

def createFolderSshCred(id, description, user, key) {
    def privateKeySource = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(key)
    def secret = new BasicSSHUserPrivateKey(CredentialsScope.GLOBAL, id, user, privateKeySource, "", id)
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.deploys(), secret)
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

def deleteFolderSshCred(id) {
    def globalCreds = getFolderCreds()
    def c = globalCreds.findResult { it.id == id ? it : null }
    if (c) {
        SystemCredentialsProvider.getInstance().getStore().removeCredentials(deploys.global(), c)
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

def deleteFolderCred(id, description, user, pass) {
    Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, user, pass)
    SystemCredentialsProvider.getInstance().getStore().removeCredentials(Domain.deploys(), c)
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

def recreateFolderCred(id, description, user, pass) {
    deleteFolderCred(id, description, user, pass)
    createFolderCred(id, description, user, pass)
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

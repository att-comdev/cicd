import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;

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

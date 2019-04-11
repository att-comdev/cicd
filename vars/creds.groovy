import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;

def getGlobalCreds() {
    return SystemCredentialsProvider.getInstance().getStore().getCredentials(Domain.global())
}

def getGlobalCredIds() {
    def globalCreds = getGlobalCreds()
    def credIds = []
    for(cred in globalCreds) {
        credIds.add(cred.id)
    }
    return credIds
}

def createGlobalCred(id, description, user, pass) {
    Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, user, pass)
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
}

def deleteGlobalCred(id, description, user, pass) {
    Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, user, pass)
    SystemCredentialsProvider.getInstance().getStore().removeCredentials(Domain.global(), c)
}

def recreateGlobalCred(id, description, user, pass) {
    deleteGlobalCred(id, description, user, pass)
    createGlobalCred(id, description, user, pass)
}

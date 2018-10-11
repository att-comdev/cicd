
def getRemote (String creds, String ip) {
    withCredentials([sshUserPrivateKey(credentialsId: creds,
        keyFileVariable: 'SSH_KEY',
        usernameVariable: 'SSH_USER')]) {

        def remote = [:]
        remote.name = 'remote'
        remote.host = ip
        remote.user = SSH_USER
        remote.identityFile = SSH_KEY
        remote.allowAnyHosts = true

        return remote
    }
}


def cmd (String creds, String ip, String cmd) {
    getRemote(creds, ip)
    sshCommand remote: remote, command: cmd
}

def wait (String creds, String ip ) {
    retry (12) {
        try {
            cmd (creds, ip, 'hostname')
        } catch (err) {
            sleep 60
            error(err)
        }
    }
}

def get (String creds, String ip, String src, String dst) {
    getRemote(creds, ip)
    sshGet remote: remote, from: src, into: dst, override: true
}

def put (String creds, String ip, String src, String dst) {
    getRemote(creds, ip)
    sshPut remote: remote, from: src, into: dst
}

def script (String creds, String ip, String script) {
    getRemote(creds, ip)
    sshScript remote: remote, script: script
}


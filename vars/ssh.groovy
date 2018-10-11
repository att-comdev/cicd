
def getRemote (String creds, String ip) {
    withCredentials([sshUserPrivateKey(credentialsId: creds,
        keyFileVariable: 'SSH_KEY',
        usernameVariable: 'SSH_USER')]) {

        def remote = [:]
        remote.name = 'remote'
        remote.host = ip
        remote.allowAnyHosts = true

        remote.user = SSH_USER

        def key = readFile SSH_KEY
        remote.identity = key

        return remote
    }
}


def cmd (String creds, String ip, String cmd) {
    sshCommand remote: getRemote(creds, ip), command: cmd
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
    sshGet remote: getRemote(creds, ip), from: src, into: dst, override: true
}

def put (String creds, String ip, String src, String dst) {
    sshPut remote: getRemote(creds, ip), from: src, into: dst
}

def script (String creds, String ip, String script) {
    sshScript remote: getRemote(creds, ip), script: script
}


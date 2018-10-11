
def cmd (String creds, String ip, String cmd) {
    withCredentials([sshUserPrivateKey(credentialsId: creds,
        keyFileVariable: 'SSH_KEY',
        usernameVariable: 'SSH_USER')]) {

        def remote = [:]
        remote.name = 'genesis'
        remote.host = ip
        remote.user = SSH_USER
        remote.identityFile = SSH_KEY
        remote.allowAnyHosts = true

        sshCommand remote: remote, command: cmd
    }
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

def put (String creds, String ip, String src, String dst) {
    withCredentials([sshUserPrivateKey(credentialsId: creds,
        keyFileVariable: 'SSH_KEY',
        usernameVariable: 'SSH_USER')]) {

        def remote = [:]
        remote.name = 'genesis'
        remote.host = ip
        remote.user = SSH_USER
        remote.identityFile = SSH_KEY
        remote.allowAnyHosts = true

        sshPut remote: remote, from: src, into: dst
    }
}

def get (String creds, String ip, String src, String dst) {
    withCredentials([sshUserPrivateKey(credentialsId: creds,
        keyFileVariable: 'SSH_KEY',
        usernameVariable: 'SSH_USER')]) {

        def remote = [:]
        remote.name = 'genesis'
        remote.host = ip
        remote.user = SSH_USER
        remote.identityFile = SSH_KEY
        remote.allowAnyHosts = true

        sshGet remote: remote, from: src, into: dst, override: true
    }
}

def script (String creds, String ip, String script) {
    withCredentials([sshUserPrivateKey(credentialsId: creds,
        keyFileVariable: 'SSH_KEY',
        usernameVariable: 'SSH_USER')]) {

        def remote = [:]
        remote.name = 'genesis'
        remote.host = ip
        remote.user = SSH_USER
        remote.identityFile = SSH_KEY
        remote.allowAnyHosts = true

        sshScript remote: remote, script: script
    }
}


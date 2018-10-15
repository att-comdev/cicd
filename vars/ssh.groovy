
// wrapper for https://jenkins.io/doc/pipeline/steps/ssh-steps/
// requires 'SSH Steps Plugin' to be installed in Jenkins

/**
 * Helper function to form remote object for the plugin
 * https://github.com/jenkinsci/ssh-steps-plugin#remote
 *
 * @param creds Jenkins credentials ID setup for SSH
 * @param ip The IP address or Hostname of the node
 */
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
    def remote = getRemote(creds, ip)
    sshCommand remote: remote, command: cmd
}


/**
 * Function used to wait and retry execution of a command.
 * Good for waiting on the node to boot/restart and become available
 */
def wait (String creds, String ip, String command) {
    retry (12) {
        try {
            cmd (creds, ip, command)
        } catch (err) {
            sleep 60
            error(err)
        }
    }
}

def get (String creds, String ip, String src, String dst) {
    def remote = getRemote(creds, ip)
    sshGet remote: remote, from: src, into: dst, override: true
}

def put (String creds, String ip, String src, String dst) {
    def remote = getRemote(creds, ip)
    sshPut remote: remote, from: src, into: dst
}

def script (String creds, String ip, String script) {
    def remote = getRemote(creds, ip)
    sshScript remote: remote, script: script
}


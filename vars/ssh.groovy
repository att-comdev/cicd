
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

def cmd (String creds, String ip, String cmd, attempts = 3, timeout = 60) {
    def remote = getRemote(creds, ip)
    retry(attempts) {
        try {
            sshCommand remote: remote, command: cmd
        } catch (err) {
            print "SSH 'command' failed."
            sleep timeout
            error(err)
        }
    }
}


/**
 * Function used to wait and retry execution of a command.
 * Good for waiting on the node to boot/restart and become available
 */
def wait (String creds, String ip, String command, attempts = 12, timeout = 60) {
    retry (attempts) {
        try {
            cmd (creds, ip, command)
        } catch (err) {
            print "SSH 'wait' failed."
            sleep timeout
            error(err)
        }
    }
}

def get (String creds, String ip, String src, String dst, attempts = 3, timeout = 60) {
    def remote = getRemote(creds, ip)
    retry(attempts) {
        try {
            sshGet remote: remote, from: src, into: dst, override: true
        } catch (err) {
            print "SSH 'get' failed."
            sleep timeout
            error(err)
        }
    }
}

def put (String creds, String ip, String src, String dst, attempts = 3, timeout = 60) {
    def remote = getRemote(creds, ip)
    retry(attempts) {
        try {
            sshPut remote: remote, from: src, into: dst
        } catch (err) {
            print "SSH 'put' failed."
            sleep timeout
            error(err)
        }
    }
}

def script (String creds, String ip, String script, attempts = 3, timeout = 60) {
    def remote = getRemote(creds, ip)
    retry(attempts) {
        try {
            sshScript remote: remote, script: script
        } catch (err) {
            print "SSH 'script' failed."
            sleep timeout
            error(err)
        }
    }
}

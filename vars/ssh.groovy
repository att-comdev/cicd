
// wrapper for https://jenkins.io/doc/pipeline/steps/ssh-steps/
// requires 'SSH Steps Plugin' to be installed in Jenkins

def _checkInputMapForKeys (Map map) {
    if (!map.containsKey('creds') || !map.containsKey('ip')) {
        error("Must provide: 'creds', 'ip'")
    }

    if (map.containsKey('proxy') && map.proxy != null && map.proxy.size() > 0) {
        if (!map.proxy.containsKey('proxyHost') || !map.proxy.containsKey('proxyPort')) {
            error("Must provide: 'proxyHost', 'proxyPort'")
        }
    }
}

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

def getRemote (Map map) {

    _checkInputMapForKeys(map)

    withCredentials([sshUserPrivateKey(credentialsId: map.creds,
        keyFileVariable: 'SSH_KEY',
        usernameVariable: 'SSH_USER')]) {

        def remote = [:]
        remote.name = 'remote'
        remote.host = map.ip
        remote.allowAnyHosts = true

        remote.user = SSH_USER

        def key = readFile SSH_KEY
        remote.identity = key

        // proxy data, if included
        if (map.containsKey('proxy') && map.proxy != null && map.proxy.size() > 0) {
            def proxy = [:]
            proxy.name = 'proxy'
            proxy.host = map.proxy.proxyHost
            proxy.port = map.proxy.proxyPort
            proxy.type = 'HTTP'
            remote.proxy = proxy
        }

        return remote
    }
}

def cmd (String creds, String ip, String cmd) {
    def remote = getRemote(creds, ip)
    sshCommand remote: remote, command: cmd
}

def cmd (Map map) {

    if (!map.containsKey('cmd')) {
        error("Must provide: 'cmd'")
    }

    def remote = getRemote(map)
    sshCommand remote: remote, command: map.cmd
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
            sleep timeout
            error(err)
        }
    }
}

def wait (Map map) {

    def attempts = 12
    def timeout = 60

    if (map.containsKey('attempts')) {
        attempts = map.attempts
    }

    if (map.containsKey('timeout')) {
        timeout = map.timeout
    }

    retry (attempts) {
        try {
            cmd (map)
        } catch (err) {
            sleep timeout
            error(err)
        }
    }
}

def get (String creds, String ip, String src, String dst) {
    def remote = getRemote(creds, ip)
    sshGet remote: remote, from: src, into: dst, override: true
}

def get (Map map) {

    if (!map.containsKey('src') || !map.cotainsKey('dst')) {
        error("Must provide: 'src', 'dst'")
    }

    def remote = getRemote(map)
    sshGet remote: remote, from: map.src, into: map.dst, override: true
}

def put (String creds, String ip, String src, String dst) {
    def remote = getRemote(creds, ip)
    sshPut remote: remote, from: src, into: dst
}

def put (Map map) {

    if (!map.containsKey('src') || !map.cotainsKey('dst')) {
        error("Must provide: 'src', 'dst'")
    }

    def remote = getRemote(map)
    sshPut remote: remote, from: map.src, into: map.dst
}

def script (String creds, String ip, String script) {
    def remote = getRemote(creds, ip)
    sshScript remote: remote, script: script
}

def script (Map map) {

    if (!map.containsKey('script')) {
        error("Must provide: 'script'")
    }

    def remote = getRemote(map)
    sshScript remote: remote, script: map.script
}

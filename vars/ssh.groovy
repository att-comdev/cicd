
// wrapper for https://jenkins.io/doc/pipeline/steps/ssh-steps/
// requires 'SSH Steps Plugin' to be installed in Jenkins

def _checkInputMapForKeys (Map map) {

    if (!map.containsKey('sshDetails')) {
        error("Must provide: 'sshDetails'")
    }

    if (!map.sshDetails.containsKey('creds') || !map.sshDetails.containsKey('ip')) {
        error("Must provide: 'sshDetails.creds', 'sshDetails.ip'")
    }

    if (map.sshDetails.containsKey('proxy') && map.sshDetails.proxy != null && map.sshDetails.proxy.size() > 0) {
        if (!map.sshDetails.proxy.containsKey('proxyHost') || !map.sshDetails.proxy.containsKey('proxyPort')) {
            error("Must provide: 'sshDetails.proxy.proxyHost', 'sshDetails.proxy.proxyPort'")
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

    withCredentials([sshUserPrivateKey(credentialsId: hostMap.creds,
        keyFileVariable: 'SSH_KEY',
        usernameVariable: 'SSH_USER')]) {

        def remote = [:]
        remote.name = 'remote'
        remote.host = hostMap.ip
        remote.allowAnyHosts = true

        remote.user = SSH_USER

        def key = readFile SSH_KEY
        remote.identity = key

        // proxy data, if included
        if (hostMap.containsKey('proxy') && hostMap.proxy != null && hostMap.proxy.size() > 0) {
            def proxy = [:]
            proxy.name = 'proxy'
            proxy.host = hostMap.proxy.proxyHost
            proxy.port = hostMap.proxy.proxyPort
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

    _checkInputMapForKeys(map)

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

    _checkInputMapForKeys(map)

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

    _checkInputMapForKeys(map)

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

    _checkInputMapForKeys(map)

    if (!map.containsKey('script')) {
        error("Must provide: 'script'")
    }

    def remote = getRemote(map)
    sshScript remote: remote, script: map.script
}

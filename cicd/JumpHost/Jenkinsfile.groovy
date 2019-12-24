CONFIG_HEADER = """
global
       daemon
       maxconn 10000

defaults
       timeout connect 500s
       timeout client 5000s
       timeout server 1h
"""

def getConfigSection(Integer octet) {
    def frontend = "frontend-${octet}"
    def backend = "backend-${octet}"
    def server = "server-${octet}"
    def ip = "10.0.0.${octet}"
    def port = 10000 + octet
    return """
frontend ${frontend}
       bind *:${port}
       default_backend ${backend}
       timeout client 1h

backend ${backend}
       mode tcp
       server ${server} ${ip}:22
"""
}


def compileConfig() {
    def config = CONFIG_HEADER
    for (it in 2..254) {
        config += getConfigSection(it)
    }
    writeFile file: "myhaproxy.cfg", text: config
}


vm (doNotDeleteNode: true, publicNet: "routable") {
    sh "sudo bash -c 'apt-get update && apt-get install haproxy -y'"
    compileConfig()
    sh "sudo cp myhaproxy.cfg /etc/haproxy/haproxy.cfg"
    sh "sudo service haproxy restart"
}

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


def compileConfig(configHeader) {
    def config = configHeader
    for (it in 2..254) {
        config += getConfigSection(it)
    }
    writeFile file: "myhaproxy.cfg", text: config
}


vm (doNotDeleteNode: true, useJumphost: false) {
    compileConfig(CONFIG_HEADER)
    sh '''
        export DEBIAN_FRONTEND=noninteractive
        sudo apt-get update
        sudo apt-get install haproxy iptables-persistent -y'"
        sudo cp myhaproxy.cfg /etc/haproxy/haproxy.cfg"
        sudo service haproxy restart"

        echo "====== Setup insterfaces ======"
        netfile=$(find /etc/network/interfaces.d -name "*.cfg")
        for interface in $(ls -1 /sys/class/net | grep ens); do
          if [ $interface != "ens3" ];then
            sudo bash -c "echo 'auto $interface' >> ${netfile}"
            sudo bash -c "echo 'iface $interface inet dhcp' >> ${netfile}"
            sudo ifdown $interface
            sudo ifup $interface
          fi
        done

        sudo iptables -A FORWARD -i ens4 -o ens3 -j ACCEPT
        sudo iptables -A FORWARD -i ens3 -o ens4 -m state --state ESTABLISHED,RELATED -j ACCEPT
        sudo iptables -t nat -A POSTROUTING -o ens3 -j MASQUERADE
        sudo iptables-save > /etc/iptables/rules.v4
    '''
}

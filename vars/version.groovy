def getShipyardCreds(siteName, sitePath=".") {
    def shipyard = readYaml file: "${sitePath}/site/${siteName}/secrets/passphrases/ucp_shipyard_keystone_password.yaml"
    return shipyard.data
}


def getIPMICreds(siteName, sitePath=".") {
    def ipmi = readYaml file: "${sitePath}/site/${siteName}/secrets/passphrases/ipmi_admin_password.yaml"
    return ipmi.data
}

def getGenesisHostname(siteName, sitePath=".") {
    def data = readYaml file: "${sitePath}/site/${siteName}/networks/common-addresses.yaml"
    return data.data.genesis.hostname
}

def getCoreDNS(siteName, sitePath=".") {
    def data = readYaml file: "${sitePath}/site/${siteName}/networks/common-addresses.yaml"
    return data.data.dns.service_ip
}

def getIPMIIPs(siteName, sitePath=".") {
    def ips = []
    patterns = ["rack*.yaml", "nodes.yaml"]
    patterns.each {
        files = findFiles(glob: "${sitePath}/site/${siteName}/baremetal/${it}")
        files.each {
                print "Reading file -> ${it}"
                data = readYaml file: it.path
                if ( !(data instanceof List)) {
                  data = [data]
                }
                data.each {
                    it.data.addressing.each {
                        if (it.network.contains("oob")) {
                           ips = ips << it.address
                        }
                    }
                }
        }
    }
    return ips
}

def getGenesisOamIp(siteName, sitePath=".") {
      def ip
      patterns = ["rack*.yaml", "networks.yaml"]
      patterns.each {
          files = findFiles(glob: "${sitePath}/site/${siteName}/networks/physical/${it}")
          files.each {
              def data = readYaml file: it.path
              if ( !(data instanceof List)) {
                data = [data]
              }
              data.each {
                  if (it.metadata.name.contains("oam")) {
                      it.data.ranges.each {
                          if (it.type == "static") {
                              ip = ip ?: it.start
                          }
                      }
                  }
              }
          }
      }
      return ip
}

def getCephDisks(siteName, sitePath=".") {
    def disks = []
    def data = readYaml file: "${sitePath}/site/${siteName}/software/charts/ucp/ceph/ceph-osd.yaml"
    data.data.values.conf.storage.osd.each {
        disks = disks << it.data.location[-1..-1]
    }
    return disks
}

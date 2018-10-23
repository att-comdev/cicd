def getShipyardCreds(siteName, sitePath=".") {
    def shipyard = readYaml file: "${sitePath}/site/${siteName}/secrets/passphrases/ucp_shipyard_keystone_password.yaml"
    return shipyard.data
}


def getIPMICreds(siteName, sitePath=".") {
    def ipmi = readYaml file: "${sitePath}/site/${siteName}/secrets/passphrases/ipmi_admin_password.yaml"
    return ipmi.data
}

def getDomain(siteName, sitePath=".") {
    def data = readYaml file: "${sitePath}/site/${siteName}/networks/common-addresses.yaml"
    return data.data.dns.ingress_domain
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
                    name = it.metadata.name
                    it.data.addressing.each {
                        res = ["name": name,
                               "ips": ["network": it.network,
                                       "address": it.address]
                        ]
                        if (it.network.contains("oob")) {
                           ips = ips << res
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
        disks = disks << it.data.location
    }
    return disks
}

def generateYaml(siteName, sitePath=".") {
    genesis_hostname = getGenesisHostname(siteName, sitePath)
    genesis_oam_ip = getGenesisOamIp(siteName, sitePath)
    genesis_node = ["name": genesis_hostname,
                    "ips": ["network": "oob", "address": "GENESIS_IPMI"],
                    "metadata": ["tags": "genesis",
                                 "genesis_oam_ip": genesis_oam_ip]
    ]
    ipmis = getIPMIIPs(siteName, sitePath)
    node = ipmis
    node = node << genesis_node
    core_dns = getCoreDNS(siteName, sitePath)
    disks = getCephDisks(siteName, sitePath)
    res = ["schema": "cicd/DeploymentHelper/v1",
           "metadata": ["schema": "metadata/Document/v1",
                        "name": "deployment-helper",
                        "layeringDefinition": ["abstract": false,
                                               "layer": "site"],
                        "storagePolicy": "cleartext"],
           "data": ["node": node,
                    "dns_service_ip": core_dns,
                    "ceph": disks]
    ]
    return res
}

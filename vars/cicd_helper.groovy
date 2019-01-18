/**
 * Retrive shipyard pasword for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return password value
 */
def getShipyardCreds(siteName, sitePath=".") {
    def shipyard = readYaml file: "${sitePath}/site/${siteName}/secrets/passphrases/ucp_shipyard_keystone_password.yaml"
    return shipyard.data
}


/**
 * Retrive IPMI pasword for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return password value
 */
def getIPMICreds(siteName, sitePath=".") {
    def ipmi = readYaml file: "${sitePath}/site/${siteName}/secrets/passphrases/ipmi_admin_password.yaml"
    return ipmi.data
}

/**
 * Retrive genesis domain name for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return domain name
 */
def getDomain(siteName, sitePath=".") {
    def data = readYaml file: "${sitePath}/site/${siteName}/networks/common-addresses.yaml"
    return data.data.dns.ingress_domain
}

/**
 * Retrive genesis host name for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return host name
 */
def getGenesisHostname(siteName, sitePath=".") {
    def data = readYaml file: "${sitePath}/site/${siteName}/networks/common-addresses.yaml"
    return data.data.genesis.hostname
}

/**
 * Retrive DNS address for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return address
 */
def getCoreDNS(siteName, sitePath=".") {
    def data = readYaml file: "${sitePath}/site/${siteName}/networks/common-addresses.yaml"
    return data.data.dns.service_ip
}

/**
 * Retrive IPMI IP addresses for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return List of IP addresses
 */
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

/**
 * Retrive genesis OAM IP addresses for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return IP address
 */
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

/**
 * Retrive ceph disks names for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return List of ceph disks names
 */
def getCephDisks(siteName, sitePath=".") {
    def disks = []
    def data = readYaml file: "${sitePath}/site/${siteName}/software/charts/ucp/ceph/ceph-osd.yaml"
    data.data.values.conf.storage.osd.each {
        disks = disks << it.data.location
    }
    return disks
}

/**
 * Generates YAML config from site manifests for given site
 * @param siteName Site name such as mtn15b.1
 * @param sitePath Path with site manifests. Such as aic-clcp-site-manifests.
 * @return YAML config
 */
def generateYaml(siteName, sitePath=".") {
    genesis_hostname = getGenesisHostname(siteName, sitePath)
    genesis_oam_ip = getGenesisOamIp(siteName, sitePath)
    genesis_node = ["name": genesis_hostname,
                    "ips": ["network": "oob", "address": null],
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

/**
 * k8s pod as Jenkins executor helper method for said pod configuration
 * @param runAsUid UID that is performing actions inside the pod
 * @param nodeSelectorLabel Determines which node(s) these pods should spin up on
 * @return pod desription
 */
def podExecutorConfig(runAsUid="0", nodeSelectorLabel="jenkins-node: enabled") {
    uid = runAsUid.trim()
    label = nodeSelectorLabel.trim()

    return """
    apiVersion: v1
    kind: Pod
    spec:
      securityContext:
        runAsUser: ${uid}
      nodeSelector:
        ${label}
    """
}

import groovy.json.JsonSlurper

def imagesJson = '''{"kolla": [{
            "repo": "openstack",
            "services": [{
                           "name": "cinder",
                           "images": ["cinder-api"]
                        },
                        {
                           "name": "heat",
                           "images": ["heat-engine", "heat-api"]
                        },
                        {
                           "name": "glance",
                           "images": ["glance-api",
                                      "glance-registry"]
                        },
                        {
                           "name": "horizon",
                           "images": ["horizon"]
                        },
                        {
                           "name": "keystone",
                           "images": ["keystone"]
                        },
                        {
                           "name": "neutron",
                           "images": ["neutron-server",
                                    "neutron-dhcp-agent",
                                    "neutron-metadata-agent",
                                    "neutron-13-agent",
                                    "neutron-openvswitch-agent",
                                    "neutron-linuxbridge-agent"]
                        },
                        {
                           "name": "nova",
                           "images": ["nova-api",
                                    "nova-conducter",
                                    "nova-scheduler",
                                    "nova-novncproxy",
                                    "nova-consoleauth",
                                    "nova-compute",
                                    "nova-ssh"]
                        },
                        {
                           "name": "barbican",
                           "images": ["barbican-api"]
                        },
                        {
                           "name": "rally",
                           "images": ["rally"]
                        }]
        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.kolla) {
    for (image in entry.images) {
        pipelineJob("${entry.repo}/${image}") {

            triggers {
                gerritTrigger {
                    serverName('')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                          pattern("${entry.repo}/${image}")
                            branches {
                                branch {
                                compareType("ANT")
                                pattern("**")
                                }
                            }

                            disableStrictForbiddenFileVerification(false)
                        }
                    }
                    triggerOnEvents {
                        patchsetCreated {
                            excludeDrafts(false)
                            excludeTrivialRebase(false)
                            excludeNoCodeChange(false)
                        }
                        changeMerged()
                    }
                }

                definition {
                    cps {
                        script(readFileFromWorkspace('kolla-newton/Jenkinsfile'))
                        sandbox()
                    }
                }
            }
        }
    }
}

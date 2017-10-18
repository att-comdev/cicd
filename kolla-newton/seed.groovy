import groovy.json.JsonSlurper

def chartsJson = '''{ "kollabuild":[{
                        "repo":"att-comdev/kolla-newton",
                        "charts":[    "cinder":
                                                ["cinder-api"],
                                     "heat":
                                                ["heat-engine",
                                                "heat-api"],
                                     "glance":
                                                ["glance-api",
                                                "glance-registry"],
                                     "horizon":
                                                ["horizon"],
                                     "keystone":
                                                ["keystone"],
                                     "neutron":
                                                ["neutron-server",
                                                "neutron-dhcp-agent",
                                                "neutron-metadata-agent",
                                                "neutron-13-agent",
                                                "neutron-openvswitch-agent",
                                                "neutron-linuxbridge-agent"],
                                     "nova":
                                                ["nova-api",
                                                "nova-conducter",
                                                "nova-scheduler",
                                                "nova-novncproxy",
                                                "nova-consoleauth",
                                                "nova-compute",
                                                "nova-ssh"],
                                     "barbican":
                                                ["barbican-api"],
                                     "rally":
                                                ["rally"]]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

for (entry in object.kollabuild) {
    for (chart in entry.charts) {
        pipelineJob("kollabuild/${entry.repo}/${chart}") {

            triggers {
                gerritTrigger {
                    serverName('Gerrithub-jenkins')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("${entry.repo}")
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

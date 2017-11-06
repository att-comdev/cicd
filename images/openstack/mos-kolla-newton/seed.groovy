import groovy.json.JsonSlurper

def imagesJson = '''{ "kolla":[{
                        "repo":"openstack",
                        "services":[ "cinder",
                                     "ceilometer",
                                     "heat",
                                     "glance",
                                     "horizon",
                                     "murano",
                                     "mistral",
                                     "keystone",
                                     "neutron",
                                     "nova",
                                     "swift",
                                     "tempest",
                                     "trove"]
             }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.kolla) {
    for (service in entry.services) {
        pipelineJob("images/${entry.repo}/mos-kolla-newton/${service}") {

            triggers {
                gerritTrigger {
                    serverName('internal-gerrit')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                          pattern("mos-${service}")
                            branches {
                                branch {
                                compareType("ANT")
                                pattern("main/newton")
                                }
                            }

                            disableStrictForbiddenFileVerification(false)
                        }
                    }
                    triggerOnEvents {
                        changeMerged()
                    }
                }

                definition {
                    cps {
                        script(readFileFromWorkspace('images/openstack/mos-kolla-newton/Jenkinsfile'))
                        sandbox()
                    }
                }
            }
        }
    }
}

import groovy.json.JsonSlurper

def imagesJson = readFileFromWorkspace('openstack/kolla-newton/images.json')

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.kolla) {
    for (service in entry.services) {
        pipelineJob("images/${entry.repo}/kolla-newton/${service}") {

            triggers {
                gerritTrigger {
                    serverName('')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                          pattern("${entry.repo}/${service}")
                            branches {
                                branch {
                                compareType("ANT")
                                pattern("stable/newton")
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

import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":["armada",
                                "deckhand",
                                "dridock",
                                "promenade",
                                "shipyard"]}'''

def Json = jsonSlurper.parseText(MySuperJson)

folder("Docker")
for (project in Json.projects) {
    pipelineJob("Docker/${project}") {
        triggers {
            gerritTrigger {
                //FIXME!!! silent mode == true
                silentMode(true)
                serverName('Gerrithub-jenkins')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/${project}")
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
                        excludeDrafts(true)
                        excludeTrivialRebase(true)
                        excludeNoCodeChange(true)
                     }
                    changeMerged()
                }
            }
            definition {
                cps {
                    script(readFileFromWorkspace('docker/Jenkinsfile'))
                    sandbox()
                }
            }
        }
    }
}


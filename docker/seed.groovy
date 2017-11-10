import groovy.json.JsonSlurper

def chartsJson = '''{"Docker":[{
                        "prefix":"att-comdev",
                        "project":[
                            "armada",
                            "deckhand",
                            "dridock",
                            "promenade",
                            "shipyard"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

folder("Docker/${entry.prefix}")
for (entry in object.Docker) {
    for (project in entry.prefix) {
        pipelineJob("Docker/${entry.prefix}/${project}") {
            triggers {
                gerritTrigger {
                    //FIXME!!! silent mode == true
                    silentMode(true)
                    serverName('Gerrithub-jenkins')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("${entry.prefix}/${project}")
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
}

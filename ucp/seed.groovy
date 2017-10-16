import groovy.json.JsonSlurper

def componentsJson = '''{ "UCP":[{
                        "repo":"att-comdev/cicd",
                        "components":[  "deckhand",
                                    "promenade",
                                    "armada",
                                    "drydock"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(componentsJson)

for (entry in object.UCP) {
    for (component in entry.components) {
        pipelineJob("UCP/${entry.repo}/${component}") {

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
                            filePaths {
                                filePath {
                                compareType("ANT")
                                pattern("$components/**")
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
                        script(readFileFromWorkspace("ucp/${component}/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}


import groovy.json.JsonSlurper

def componentsJson = '''{ "UCP":[{
                        "repo":"att-comdev/cicd/ucp",
                        "components":[  "deckhand",
                                    "promenade",
                                    "ucp-armada/armada",
                                    "ucp-drydock/drydock"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(componentsJson)

for (entry in object.UCP) {
    for (item in entry.components) {
        pipelineJob("UCP/${entry.repo}/${item}") {

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
                                pattern("$chart/**")
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
                        for (entry in object.UCP) {
                            for (chart in entry.charts) {
                        script(readFileFromWorkspace('ucp/${entry.repo}/${item}/Jenkinsfile'))
                        sandbox()
                        }
                        }
                    }
                }
            }
        }
    }
}


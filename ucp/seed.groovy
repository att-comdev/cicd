import groovy.json.JsonSlurper

def chartsJson = '''{ "UCP":[{
                        "repo":"att-comdev",
                        "charts":[  "deckhand",
                                    "promenade",
                                    "armada",
                                    "drydock"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

for (entry in object.UCP) {
    for (chart in entry.charts) {
        pipelineJob("UCP/${chart}/${chart}") {

            triggers {
                gerritTrigger {
                    serverName('Gerrithub-jenkins')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("${entry.repo}/${chart}")
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

                        script(readFileFromWorkspace("ucp/${chart}/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}


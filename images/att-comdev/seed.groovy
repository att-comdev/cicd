import groovy.json.JsonSlurper

def imagesJson = '''{ "UCP":[{
                        "repo":"att-comdev",
                        "images":[
                                  "drydock",
                                  "armada"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.UCP) {
    for (image in entry.images) {
        pipelineJob("UCP/${image}/${image}") {
            triggers {
                gerritTrigger {
                    serverName('Gerrithub-jenkins')
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
                          customUrl("$NEXUS3_URL/repository/att-comdev-jenkins-logs/att-comdev/${image}/\$BUILD_NUMBER/${image}-\$BUILD_NUMBER")
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
                      script(readFileFromWorkspace("images/${entry.repo}/${image}/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}
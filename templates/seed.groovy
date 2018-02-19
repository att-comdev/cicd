import groovy.json.JsonSlurper

def imagesJson = '''{ "entries":[{
                        "repo":"att-comdev",
                        "images":[
                                  "my-component1",
                                  "my-component2",
                                  "my-component3",
                                  "my-component4"
                                  ]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.entries) {
    for (image in entry.images) {
      pipelineJob("images/${entry.repo}/${image}/${image}") {
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


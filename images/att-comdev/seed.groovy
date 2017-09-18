import groovy.json.JsonSlurper

def imagesJson = '''{ "UCP":[{
                        "repo":"att-comdev",
                        "images":[
                                  "drydock"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.UCP) {
    for (image in entry.images) {
        pipelineJob("UCP/${image}/${image}") {
            parameters {
                stringParam {
                    defaultValue(GERRIT_REFSPEC)
                    description('Pass att-comdev/cicd code refspec to the job')
                    name ('CICD_GERRIT_REFSPEC')
                }
            }
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
                        script(readFileFromWorkspace("ucp/${image}/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}
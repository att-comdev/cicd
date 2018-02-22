import groovy.json.JsonSlurper

def imagesJson = '''{ "UCP":[{
                        "repo":"att-comdev",
                        "images":[
                                  "shipyard"
                                  ]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.UCP) {
    for (image in entry.images) {
      pipelineJob("images/${entry.repo}/${image}/${image}") {
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
                      script(readFileFromWorkspace("images/${entry.repo}/${image}/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}
imagesJson = '''{ "github":[{
                        "repo":"att-comdev",
                        "images":[
                                  "shipyard",
                                  "drydock",
                                  "maas",
                                  "deckhand",
                                  "armada"
                                  ]
                        }]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
    for (image in entry.images) {
      pipelineJob("images/${entry.repo}/${image}/${image}-master") {
            parameters {
                stringParam {
                    defaultValue("${image}")
                    description('Name of repo in att-comdev to build')
                    name ('PROJECT_NAME')
                }
            }
            scm {
               github('${entry.repo}/${image}')
            }

            definition {
               cps {
                   script(readFileFromWorkspace("images/${entry.repo}/Jenkinsfile"))
                   sandbox()
               }
            }
        }
    }
}

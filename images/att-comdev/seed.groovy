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
                               "repo":"att-comdev/shipyard",
                               "image":"shipyard"
                            },{
                               "repo":"att-comdev/shipyard",
                               "image":"airflow"
                            },{          
                               "repo":"att-comdev/drydock",
                               "image":"drydock"
                            },{
                               "repo":"att-comdev/maas",
                               "image":"sstream-cache"
                            },{
                               "repo":"att-comdev/maas",
                               "image":"maas-region-controller"
                            },{
                               "repo":"att-comdev/maas",
                               "image":"maas-rack-controller"
                            },{
                               "repo":"att-comdev/deckhand",
                               "image":"deckhand"
                            },{
                               "repo":"att-comdev/armada",
                               "image":"armada"
                            }]}'''
           
jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
      pipelineJob("images/${entry.repo}/${entry.image}-master") {
            parameters {
                stringParam {
                    defaultValue("${entry.image}")
                    description('Name of repo in att-comdev to build')
                    name ('PROJECT_NAME')
                }
                stringParam {
                    defaultValue("0.1.0}")
                    description('Put RC version here')
                    name('VERSION')
                }
            }
            scm {
               github("${entry.repo}")
            }

            definition {
               cps {
                   script(readFileFromWorkspace("images/att-comdev/Jenkinsfile"))
                   sandbox()
               }
            }
        }
    }
}

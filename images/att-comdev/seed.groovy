import groovy.json.JsonSlurper

def imagesJson = '''{ "UCP":[{
                        "repo":"att-comdev",
                        "images":[
                                  "shipyard",
                                  "pegleg"
                                  ]
                        }]}'''
def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.UCP) {
    for (image in entry.images) {
        pipelineJob("images/${entry.repo}/${image}/${image}") {
            configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '3'
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
                        commentAddedContains {
                           commentAddedCommentContains('recheck')
                        }
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
                               "directory":"images/att-comdev/shipyard",
                               "image":"shipyard"
                            },{
                               "repo":"att-comdev/shipyard",
                                "directory":"images/att-comdev/shipyard/airflow",
                                "image":"airflow"
                            },{          
                               "repo":"att-comdev/drydock",
                                "directory":"images/att-comdev/drydock",
                                "image":"drydock"
                            },{
                                "repo":"att-comdev/maas",
                                "directory":"images/att-comdev/maas/sstream-cache",
                                "image":"sstream-cache"
                            },{
                                "repo":"att-comdev/maas",
                                "directory":"images/att-comdev/maas/maas-region-controller",
                                "image":"maas-region-controller"
                            },{
                                "repo":"att-comdev/maas",
                                "directory":"images/att-comdev/maas/maas-rack-controller",
                                "image":"maas-rack-controller"
                            },{
                                "repo":"att-comdev/deckhand",
                                "directory":"images/att-comdev/deckhand",
                                "image":"deckhand"
                            },{
                                "repo":"att-comdev/armada",
                                "directory":"images/att-comdev/armada",
                                "image":"armada"
                            },{
                                "repo":"att-comdev/promenade",
                                "directory":"images/att-comdev/promenade",
                                "image":"promenade"
                            }]}'''
           
jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
    pipelineJob("${entry.directory}/${entry.image}-master") {
        parameters {
            stringParam {
                defaultValue("${entry.image}")
                description('Name of repo in att-comdev to build')
                name ('PROJECT_NAME')
            }
            stringParam {
                defaultValue("0.1.0")
                description('Put RC version here')
                name('VERSION')
            }
            stringParam {
                defaultValue("*/master")
                description('Put Branch or commit id here')
                name('COMMIT_ID')
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


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

imagesJson = '''{ "attcomdev":[{
                        "repo":"att-comdev/dockerfiles",
                        "images":[
                                  "nagios"
                                  ]
                        }]}'''
jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)
for (entry in object.UCP) {
    folder("images/${entry.repo}")
    for (image in entry.images) {
        pipelineJob("images/${entry.repo}/${image}") {
            configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '5'
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
                      script(readFileFromWorkspace("images/att-comdev/${image}/Jenkinsfile"))
                      sandbox()
                    }
                }
            }
        }
    }
}

import groovy.json.JsonSlurper

def imagesJson = '''{ "images":[{
                        "repo":"airship-pegleg",
                        "pipelineNames":[
                                  "pegleg"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)


folder('images/airship/pegleg')

for (entry in object.images) {
    for (pipelineName in entry.pipelineNames) {
        pipelineJob("images/airship/pegleg/${pipelineName}") {
            configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '3'
                }
            }

            triggers {
                gerritTrigger {
                    serverName('ATT-airship-CI')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                          pattern("openstack/${entry.repo}")
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
                      script(readFileFromWorkspace("images/airship/pegleg/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}

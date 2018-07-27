import groovy.json.JsonSlurper

def imagesJson = '''{ "images":[{
                        "repo":"airship-promenade",
                        "pipelineNames":[
                                  "promenade"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)


folder('images/airship/promenade')

for (entry in object.images) {
    for (pipelineName in entry.pipelineNames) {
        pipelineJob("images/airship/promenade/${pipelineName}") {
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
                      script(readFileFromWorkspace("images/airship/promenade/${pipelineName}/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}

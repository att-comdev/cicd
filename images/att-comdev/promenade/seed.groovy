import groovy.json.JsonSlurper

def imagesJson = '''{ "images":[{
                        "repo":"att-comdev/promenade",
                        "pipelineNames":[
                                  "Promenade",
                                  "Resiliency"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)


folder('images/att-comdev/promenade')

for (entry in object.images) {
    for (pipelineName in entry.pipelineNames) {
        pipelineJob("images/${entry.repo}/${pipelineName}") {
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
                            pattern("${entry.repo}")
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
                        script(readFileFromWorkspace("images/${entry.repo}/Jenkinsfile${pipelineName}"))
                        sandbox()
                    }
                }
            }
        }
    }
}

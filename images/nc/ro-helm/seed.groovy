import groovy.json.JsonSlurper

def imagesJson = '''{ "nc":[{
                        "repo":"nc",
                        "images":[
                                  "ro-helm"]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)
folder("images/nc")
folder("images/nc/ro-helm")
for (entry in object.nc) {
    for (image in entry.images) {
      pipelineJob("images/${entry.repo}/${image}/${image}") {
            configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '4'
                }
            }
            triggers {
                gerritTrigger {
                    serverName('mtn5-gerrit')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("${image}")
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
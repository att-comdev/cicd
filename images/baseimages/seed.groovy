folder("images/base-images")

pipelineJob("images/base-images/create-ubuntu-base-image") {
    parameters {
        stringParam {
            defaultValue("att-comdev/ubuntu:xenial")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("scratch")
            description('Name of the Image to publish')
            name('BASE_IMAGE')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {
       gerritTrigger {
                silentMode(true)
                serverName('Gerrithub-jenkins-temp')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/dockerfiles")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType('REG_EXP')
                                pattern('base-images/.*')
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
               script(readFileFromWorkspace("images/baseimages/JenkinsfileMaster"))
               sandbox()
           }
       }
    }
}
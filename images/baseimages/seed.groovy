folder("images/base-images")

pipelineJob("images/base-images/create-ubuntu-base-image") {
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
               script(readFileFromWorkspace("images/baseimages/Jenkinsfile"))
               sandbox(false)
           }
       }
    }
}
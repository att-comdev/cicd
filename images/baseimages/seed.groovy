folder("images/base-images")

pipelineJob("images/base-images/buildall") {
    parameters {
        stringParam {
            defaultValue("0.9.0")
            description('Put RC version here')
            name('VERSION')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {

       definition {
           cps {
               // TBD.
               // Need to invoke the other job in a specific order
               sandbox()
           }
       }
    }
}

pipelineJob("images/base-images/master/docker-brew-ubuntu-core-xenial") {
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

pipelineJob("images/base-images/master/buildpack-deps-xenial-curl") {
    parameters {
        stringParam {
            defaultValue("att-comdev/buildpack-deps:xenial-curl")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("att-comdev/ubuntu:xenial")
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

pipelineJob("images/base-images/master/buildpack-deps-xenial-scm") {
    parameters {
        stringParam {
            defaultValue("att-comdev/buildpack-deps:xenial-scm")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("att-comdev/buildpack-deps:xenial-curl")
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

pipelineJob("images/base-images/master/buildpack-deps-xenial") {
    parameters {
        stringParam {
            defaultValue("att-comdev/buildpack-deps:xenial")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("att-comdev/buildpack-deps:xenial-scm")
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
pipelineJob("images/base-images/master/python-3.5-xenial") {
    parameters {
        stringParam {
            defaultValue("att-comdev/python:3.5")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("att-comdev/buildpack-deps:xenial")
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

pipelineJob("images/base-images/master/python-3.6-xenial") {
    parameters {
        stringParam {
            defaultValue("att-comdev/python:3.6")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("att-comdev/buildpack-deps:xenial")
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








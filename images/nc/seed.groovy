JOB_FOLDER="images/nc/aqua"
folder("images/nc")
folder("images/nc/aqua")
pipelineJob("${JOB_FOLDER}/aqua") {
    configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '3'
                }
    }
    triggers {
        gerritTrigger {
            silentMode(false)
            serverName('mtn5-gerrit')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("PUT PROJECT HERE")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('ANT')
                            pattern("**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(true)
                    excludeTrivialRebase(false)
                    excludeNoCodeChange(true)
                }
                changeMerged()
                commentAddedContains {
                   commentAddedCommentContains('recheck')
                }
            }
        }
    }
    definition {
        cps {
          script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile-AQUA"))
            sandbox()
        }
    }
}
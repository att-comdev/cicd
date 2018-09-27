folder("images/staas-backup")

pipelineJob("images/staas-backup/create-staas-backup-image") {

    displayName('Build Packages')

    description('\nThis job is supposed to build (staas-backup/) custom '+
                'Ubuntu packages and upload them to Artifactory.\nList of '+
                'packages is in Jenkinsfile, submit a change to amend it.')

    logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
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
                                pattern('staas-backup/.*')
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
               script(readFileFromWorkspace("images/staas-backup/Jenkinsfile"))
               sandbox(false)
           }
       }
    }
}

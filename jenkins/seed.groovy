JOB_FOLDER="jenkins"
pipelineJob("${JOB_FOLDER}") {
    properties {
        disableConcurrentBuilds()
    }
    logRotator{
        daysToKeep(90)
    }
    triggers {
        gerritTrigger {
            silentMode(true)
            serverName('airship-ci')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("airship/airshipctl")
                    branches {
                        branch {
                            compareType('ANT')
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
                    excludeNoCodeChange(true)
                    excludePrivateState(false)
                    excludeWipState(false)
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
          script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
            sandbox(false)
        }
    }
}

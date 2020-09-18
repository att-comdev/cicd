JOB_NAME="DeployOnCAPD"
SCRIPT_NAME="eric-cicd/CAPD/jenkins_capd"
pipelineJob("${JOB_NAME}") {
    configureJob("${SCRIPT_NAME}")
}

JOB_NAME="DeployOnCAPZ"
SCRIPT_NAME="eric-cicd/CAPZ/jenkins_capz"
pipelineJob("${JOB_NAME}") {
    configureJob("${SCRIPT_NAME}")
}

def configureJob(scriptName) {
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
          script(readFileFromWorkspace("${scriptName}"))
            sandbox(false)
        }
    }
}

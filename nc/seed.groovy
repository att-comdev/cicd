JOB_FOLDER="nc/Airshipctl"
JOB_NAME="Airshipctl"
pipelineJob("${JOB_NAME}") {
    properties {
        disableConcurrentBuilds()
    }
    logRotator{
        daysToKeep(90)
    }
    parameters {
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('master')
            description('The gerrit refspec')
            trim(true)
        }
        stringParam {
            name ('NODE_LABEL')
            defaultValue('airship1')
            description('The node label of the slave')
            trim(true)
        }
    }
    triggers {
        cron("H */4 * * *")
        gerritTrigger {
            silentMode(false)
            serverName('gerrit-service')
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
                    disableStrictForbiddenFileVerification(true)
                    forbiddenFilePaths {
                        compareType('ANT')
                        pattern("docs/**")
                    } 
                }
            }
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(false)
                    excludeTrivialRebase(false)
                    excludeNoCodeChange(false)
                    excludePrivateState(false)
                    excludeWipState(true)
                }
                changeMerged()
		draftPublished()
                commentAddedContains {
                   commentAddedCommentContains('recheck')
                }
            }
        }
    }
    definition {
        cps {
          script(readFileFromWorkspace("${JOB_FOLDER}/airshipctl"))
            sandbox(false)
        }
    }
}

JOB_FOLDER="nc/Airshipctl"
JOB_NAME="Airshipctl"
pipelineJob("${JOB_NAME}") {
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
                    forbiddenFilePaths {
		        filePath {
                            compareType('ANT')
                            pattern("docs/**")
                        }
		    }
                    disableStrictForbiddenFileVerification(true)
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
                topicChanged()
		draftPublished()
                commentAddedContains {
                   commentAddedCommentContains('recheck')
                }
            }
        }
    }
    definition {
        cps {
          script(readFileFromWorkspace("${JOB_FOLDER}/airshipctl.groovy"))
            sandbox(true)
        }
    }
}

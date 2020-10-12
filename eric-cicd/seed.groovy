JOB_FOLDER="eric-cicd/CAPD"
JOB_NAME="DeployOnCAPD"
pipelineJob("${JOB_NAME}") {
    properties {
        disableConcurrentBuilds()
    }
    logRotator{
        daysToKeep(90)
    }
    parameters {
        stringParam {
            name ('targetVM')
            defaultValue('10.1.1.31')
            description('Target VM to deploy')
            trim(true)
        }
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
          script(readFileFromWorkspace("${JOB_FOLDER}/jenkins_capd"))
            sandbox(false)
        }
    }
}

JOB_NAME="DeployOnCAPZ"
JOB_FOLDER="eric-cicd/CAPZ"
pipelineJob("${JOB_NAME}") {
    properties {
        disableConcurrentBuilds()
    }
    logRotator{
        daysToKeep(90)
    }
    definition {
        cps {
          script(readFileFromWorkspace("${JOB_FOLDER}/jenkins_capz"))
            sandbox(false)
        }
    }
}

JOB_NAME="DeployVppIpFwdOnAZ"
JOB_FOLDER="eric-cicd/CAPZ"
pipelineJob("${JOB_NAME}") {
    properties {
        disableConcurrentBuilds()
    }
    logRotator{
        daysToKeep(90)
    }
    definition {
        cps {
          script(readFileFromWorkspace("${JOB_FOLDER}/jenkins_capz_vpp_cnf"))
            sandbox(false)
        }
    }
}

JOB_FOLDER="eric-cicd/METAL3"
JOB_NAME="DeployOnBaremetal"
pipelineJob("${JOB_NAME}") {
    properties {
        disableConcurrentBuilds()
    }
    logRotator{
        daysToKeep(90)
    }
    parameters {
        stringParam {
            name ('targetVM')
            defaultValue('10.1.1.102')
            description('Target VM to deploy')
            trim(true)
        }
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
          script(readFileFromWorkspace("${JOB_FOLDER}/jenkins_metal3"))
            sandbox(false)
        }
    }
}

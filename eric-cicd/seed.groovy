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

JOB_NAME="DeployEricNrfOnAZ"
JOB_FOLDER="eric-cicd/CAPZ"
pipelineJob("${JOB_NAME}") {
    properties {
        disableConcurrentBuilds()
    }
    logRotator{
        daysToKeep(90)
    }
    parameters {
        stringParam {
            name ('AZURE_LOCATION')
            defaultValue('East US 2')
            description('Azure Region - "Central US", "West Europe", "North Central US", etc..')
            trim(true)
        }
        stringParam {
            name ('AZURE_HOST_TYPE')
            defaultValue('Standard_B4ms')
            description('Azure Host VM Type - "Standard_DS2_v2", "Standard_D32s_v3", etc..')
            trim(true)
        }
        stringParam {
            name ('AZURE_CONTROL_PLANE_MACHINE_TYPE')
            defaultValue('Standard_D32as_v4')
            description('Azure Target cluster Control Plane Machine Type - "Standard_DS2_v2", "Standard_D32s_v3", etc..')
            trim(true)
        }
        stringParam {
            name ('AZURE_NODE_MACHINE_TYPE')
            defaultValue('Standard_D32as_v4')
            description('Azure Target cluster Worker Machine Type - "Standard_DS2_v2", "Standard_D32s_v3", etc..')
            trim(true)
        }
        stringParam {
            name ('HELM_REPO_HOST')
            defaultValue('http://13.94.241.73:8081')
            description('Helm Repo uri hosting helm charts')
            trim(true)
        }
        stringParam {
            name ('MANIFEST_GIT_URL')
            defaultValue('http://10.1.1.35:3000/airship/demo.git')
            description('GIT URL hosting Eric NRF CNF manifests')
            trim(true)
        }
    }
    definition {
        cps {
          script(readFileFromWorkspace("${JOB_FOLDER}/jenkins_capz_eric_nrf_cnf"))
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
          script(readFileFromWorkspace("${JOB_FOLDER}/jenkins_m3"))
            sandbox(false)
        }
    }
}

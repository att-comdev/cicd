JOB_FOLDER="images/airship/porthole"
folder(JOB_FOLDER)
project_name = "calicoctl-utility"
JOB_BASE_NAME = project_name

pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
    description("This job builds the image for ${JOB_BASE_NAME}")
    logRotator {
        daysToKeep(90)
    }
    parameters {
        stringParam("REVISION", "", "Gerrit revision")
        stringParam("REFSPEC", "", "Gerrit revision")
        stringParam("CALICOQ_VERSION", "", "Image version calicoq binary copied from. Default is empty, version specified in repo used")
        stringParam("CALICOCTL_VERSION", "", "Image version calicoctl binary copied from. Default is empty, version specified in repo used")
    }
    triggers {
        gerritTrigger {
            silentMode(true)
            serverName('ATT-airship-CI')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("airship/porthole")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("**")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType("ANT")
                            pattern("**/Dockerfiles/${JOB_BASE_NAME}/**")
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
            }
        }
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                sandbox(false)
            }
        }
    }
}

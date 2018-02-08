JOB_FOLDER="cicd"
JOB_NAME='PythonCodeReview'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue("att-comdev/armada")
            description('Gerrit refspec or branch')
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('master')
            description('Gerrit refspec or branch')
        }
        stringParam {
            name ('GERRIT_CHANGE_NUMBER')
            defaultValue('0')
            description('patchset number')
        }
        stringParam {
            name ('GERRIT_EVENT_TYPE')
            defaultValue('patchset-created')
            description('patchset-created or change-merged')
        }
    }
    triggers {
        gerritTrigger {
            serverName('Gerrithub-jenkins')
            silentMode(false)
            gerritProjects {
                gerritProject {
                    compareType('REG_EXP')
                    pattern('^att-comdev/(?!(cicd|docker)).*')
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("**")
                        }
                    }
/*                    filePaths {
                        filePath {
                            compareType('REG_EXP')
                            pattern("${file_path}")
                        }
                        filePath {
                            compareType('REG_EXP')
                            pattern("Makefile")
                        }
                    }
*/
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                changeMerged()
                patchsetCreated {
                   excludeDrafts(true)
                   excludeTrivialRebase(true)
                   excludeNoCodeChange(true)
                }
            }
        }
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}
}

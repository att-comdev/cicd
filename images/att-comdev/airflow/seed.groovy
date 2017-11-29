JOB_FOLDER="images/att-comdev"
JOB_NAME="${JOB_FOLDER}/airflow"

folder("${JOB_FOLDER}")
pipelineJob(JOB_NAME) {
    triggers {
        gerritTrigger {
            silentMode(false)
            serverName('Gerrithub-jenkins')
            gerritProjects {
            //shipyard ps-created and merge trigger
                gerritProject {
                    compareType('PLAIN')
                    pattern('att-comdev/shipyard')
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('ANT')
                            pattern('**')
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(true)
                    excludeTrivialRebase(true)
                    excludeNoCodeChange(true)
                 }
                changeMerged()
            }
        }
        gerritTrigger {
            silentMode(true)
            serverName('Gerrithub-jenkins')
            gerritProjects {
            //armada merge trigger
                gerritProject {
                    compareType('PLAIN')
                    pattern('att-comdev/armada')
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('ANT')
                            pattern('**')
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            //drydock merge trigger
                gerritProject {
                    compareType('PLAIN')
                    pattern('att-comdev/drydock')
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('ANT')
                            pattern('**')
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                changeMerged()
            }
        }

        definition {
            cps {
                script(readFileFromWorkspace("${JOB_NAME}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}
import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

JOB_FOLDER="images/nc"
folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/aqua") {
    configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '3'
                }
    }
    triggers {
        gerritTrigger {
            silentMode(false)
            serverName('UPDATE TO MTN VARIABLE')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("PUT PROJECT HERE")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('ANT')
                            pattern("**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(true)
                    excludeTrivialRebase(false)
                    excludeNoCodeChange(true)
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
          script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile-AQUA"))
            sandbox()
        }
    }
}

pipelineJob("${JOB_FOLDER}/airflow-integration") {
    configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '3'
                }
    }
    triggers {
        gerritTrigger {
            silentMode(true)
            serverName('Gerrithub-jenkins')
            gerritProjects {
                for (project in Json.projects) {
                    gerritProject {
                        compareType('PLAIN')
                        pattern(project.repo)
                        branches {
                            branch {
                                compareType('ANT')
                                pattern("**/master")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType('ANT')
                                pattern(project.path)
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                }
            }
          customUrl("\${CUSTOM_URL}")
            triggerOnEvents {
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
            sandbox()
        }
    }
}

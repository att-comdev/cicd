import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()
def UCP_deps = '''{"projects":[
                        {"name":"armada",
                         "repo":"att-comdev/armada",
                         "path":"**"
                         },
                        {"name":"drydock",
                         "repo":"att-comdev/drydock",
                         "path":"**"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(UCP_deps)

JOB_FOLDER="images/att-comdev/shipyard/airflow"
folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/airflow") {
    configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '3'
                }
    }
    triggers {
        gerritTrigger {
            silentMode(false)
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("att-comdev/shipyard")
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
            customUrl("\${CUSTOM_URL}")
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
          script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
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
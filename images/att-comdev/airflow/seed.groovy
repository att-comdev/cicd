import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"airflow-shipyard",
                         "repo":"att-comdev/shipyard",
                         "branch":"**/master",
                         "path":"**"
                         },
                        {"name":"airflow-armada",
                         "repo":"att-comdev/armada",
                         "branch":"**/master",
                         "path":"**"
                         },
                        {"name":"airflow-drydock",
                         "repo":"att-comdev/drydock",
                         "branch":"**/master",
                         "path":"**"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(MySuperJson)

PROJECT_FOLDER="images/att-comdev"
PROJECT_NAME="airflow"

folder("${PROJECT_FOLDER}")
pipelineJob("${PROJECT_FOLDER}/${PROJECT_NAME}") {
    triggers {

// trigger for shipyard project (ps + merge):
        gerritTrigger {
            silentMode(false)
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("${Json.projects.repo[0]}")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("${Json.projects.branch[0]}")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType("ANT")
                            pattern("${projects.path[0]}")
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

// trigger for armada project (only merge):
        gerritTrigger {
            silentMode(false)
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("${Json.projects.repo[1]}")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("${Json.projects.branch[1]}")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType("ANT")
                            pattern("${Json.projects.path[1]}")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                changeMerged()
            }
        }

// trigger for drydock project (only merge):
        gerritTrigger {
            silentMode(false)
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("${Json.projects.repo[2]}")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("${Json.projects.branch[2]}")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType("ANT")
                            pattern("${Json.projects.path[2]}")
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
                script(readFileFromWorkspace("${PROJECT_FOLDER}/${PROJECT_NAME}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}


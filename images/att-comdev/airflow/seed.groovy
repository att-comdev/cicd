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
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(true)
                    excludeTrivialRebase(true)
                    excludeNoCodeChange(true)
                }
                changeMerged()
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
            triggerOnEvents {
                changeMerged()
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



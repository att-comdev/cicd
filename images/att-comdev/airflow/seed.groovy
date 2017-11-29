import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()
def AirflowJson = '''{"projects":[
                        {"name":"airflow",
                         "repo":"att-comdev/shipyard",
                         "path":"**"
                         },
                        {"name":"armada",
                         "repo":"att-comdev/armada",
                         "path":"**"
                         },
                        {"name":"drydock",
                         "repo":"att-comdev/drydock",
                         "path":"**"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(AirflowJson)

JOB_FOLDER="images/att-comdev/airflow"
folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/airflow") {
    triggers {
        gerritTrigger {
            silentMode(false)
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
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
            sandbox()
        }
    }
}



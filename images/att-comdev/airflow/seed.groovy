import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()
def AirflowJson = '''{"projects":[
                        {"name":"shipyard-airflow",
                         "repo":"att-comdev/shipyard",
                         "path":"**"
                         },
                        {"name":"armada-airflow",
                         "repo":"att-comdev/armada",
                         "path":"**"
                         },
                        {"name":"drydock-airflow",
                         "repo":"att-comdev/drydock",
                         "path":"**"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(AirflowJson)

JOB_FOLDER="images/att-comdev/airflow"
folder(JOB_FOLDER)
for (project in Json.projects){
    pipelineJob("${JOB_FOLDER}/${project.name}") {
        triggers {
            gerritTrigger {
                silentMode(false)
                serverName('Gerrithub-jenkins')
                gerritProjects {
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
    }
}

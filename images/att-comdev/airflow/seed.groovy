import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

PROJECT_FOLDER="images/att-comdev"
folder("${PROJECT_FOLDER}")

//2DO: Seed should be extended to merge events for armada and drydock

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"airflow",
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
//backup                         "path":"*/images/airflow"

def Json = jsonSlurper.parseText(MySuperJson)

PROJECT_NAME=Json.projects.name[0]
pipelineJob("${PROJECT_FOLDER}/${PROJECT_NAME}/${PROJECT_NAME}") {
    triggers {
        gerritTrigger {
            silentMode(false)
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern(Json.projects.repo[0])
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('ANT')
                            pattern(Json.projects.path[0])
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
                script(readFileFromWorkspace("${PROJECT_FOLDER}/${PROJECT_NAME}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

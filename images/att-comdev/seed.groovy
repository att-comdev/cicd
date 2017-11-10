import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"rabbitmq-dockerfiles",
                         "repo":"att-comdev/dockerfiles",
                         "branch":"**/master",
                         "path":"rabbitmq/**"
                         },
                        {"name":"maas-dockerfiles",
                         "repo":"att-comdev/dockerfiles",
                         "branch":"**/master",
                         "path":"maas/**"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(MySuperJson)

PROJECT_FOLDER="images/att-comdev"
folder("${PROJECT_FOLDER}")
for (project in Json.projects) {
    pipelineJob("${PROJECT_FOLDER}/${project.name}") {
        triggers {
            gerritTrigger {
                silentMode(false)
                serverName('Gerrithub-jenkins')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("${project.repo}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("${project.branch}")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType("ANT")
                                pattern("${project.path}")
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
                    script(readFileFromWorkspace("${PROJECT_FOLDER}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}

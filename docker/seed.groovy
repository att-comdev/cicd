import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"dockerfiles", "branch":"**/master", "path":"**"},
                        {"name":"maas",        "branch":"**/master", "path":"**"},
                        {"name":"shipyard", "branch":"**/master", "path":"att-comdev/shipyard/images/airflow/**"}
                    ]}'''

def Json = jsonSlurper.parseText(MySuperJson)

folder("Docker")
for (project in Json.projects) {
    pipelineJob("Docker/${project.name}") {
        parameters {
            stringParam {
                name ('ARTIFACTS')
                defaultValue('maas airflow rabbitmq')
                description("Artifacts we're looking for")
            }
        }
        triggers {
            gerritTrigger {
                silentMode(false)
                serverName('Gerrithub-jenkins')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/${project.name}")
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
                    script(readFileFromWorkspace('docker/Jenkinsfile'))
                    sandbox()
                }
            }
        }
    }
}


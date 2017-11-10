import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"maas/maas-rack-controller",
                         "repo":"att-comdev/maas",
                         "branch":"**/master",
                         "path":"images/maas-rack-controller"
                         },
                         {"name":"maas/maas-region-controller",
                         "repo":"att-comdev/maas",
                         "branch":"**/master",
                         "path":"images/maas-region-controller"
                         },
                         {"name":"maas/sstream-cache",
                         "repo":"att-comdev/maas",
                         "branch":"**/master",
                         "path":"images/sstream-cache"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(MySuperJson)

PROJECT_FOLDER="images/att-comdev/maas"
folder(PROJECT_FOLDER)
for (project in Json.projects) {
    pipelineJob("${PROJECT_FOLDER}/${project.name}") {
//        environmentVariables {
//            env('GERRIT_EVENT_TYPE',"manual run")
//            env('GERRIT_CHANGE_NUMBER','none')
//            keepBuildVariables(true)
//        }
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
                                pattern("**")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType("ANT")
                                pattern("${project.path}")
                            }
                            filePath {
                                compareType("ANT")
                                pattern("Makefile")
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

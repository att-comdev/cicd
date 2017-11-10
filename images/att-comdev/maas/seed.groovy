import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"maas-rack-controller",
                         "repo":"att-comdev/maas",
                         "branch":"**/master",
                         "path":"images/maas-rack-controller"
                         },
                         {"name":"maas-region-controller",
                         "repo":"att-comdev/maas",
                         "branch":"**/master",
                         "path":"images/maas-region-controller"
                         },
                         {"name":"sstream-cache",
                         "repo":"att-comdev/maas",
                         "branch":"**/master",
                         "path":"images/sstream-cache"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(MySuperJson)

JOB_FOLDER="images/att-comdev/maas"
folder(JOB_FOLDER)
for (project in Json.projects) {
    pipelineJob("${JOB_FOLDER}/${project.name}") {
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
                    commentAddedContains {
                       commentAddedCommentContains('recheck')
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
    }
}

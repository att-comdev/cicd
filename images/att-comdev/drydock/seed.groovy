import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"drydock",
                         "repo":"att-comdev/drydock",
                         "branch":"**/master",
                         "path":"**"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(MySuperJson)

JOB_FOLDER="images/att-comdev/drydock"
folder(JOB_FOLDER)
for (project in Json.projects) {
    pipelineJob("${JOB_FOLDER}/${project.name}") {
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '4'
            }
        }
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
                        excludeTrivialRebase(false)
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

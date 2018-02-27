import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"drydock",
                         "repo":"att-comdev/drydock",
                         "branch":"**/master",
                         "path":"**"
                         },
                         {"name":"deckhand",
                         "repo":"att-comdev/deckhand",
                         "branch":"**/master",
                         "path":"**"
                         },
                         {"name":"shipyard",
                         "repo":"att-comdev/shipyard",
                         "branch":"**/master",
                         "path":"**"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(MySuperJson)
for (project in Json.projects) {
    pipelineJob("${project.repo}/${project.name}") {
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
                  script(readFileFromWorkspace("${project.repo}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}

import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new projects here:
def MySuperJson = '''{"projects":[
                        {"name":"maas-rack-controller",
                         "repo":"openstack/airship-maas",
                         "branch":"**",
                         "path":"**"
                         },
                         {"name":"maas-region-controller",
                         "repo":"openstack/airship-maas",
                         "branch":"**",
                         "path":"**"
                         },
                         {"name":"sstream-cache",
                         "repo":"openstack/airship-maas",
                         "branch":"**",
                         "path":"**"
                         }
                    ]}'''

def Json = jsonSlurper.parseText(MySuperJson)

JOB_FOLDER="images/airship/maas"

folder(JOB_FOLDER)
folder("${JOB_FOLDER}/maas-rack-controller")
folder("${JOB_FOLDER}/maas-region-controller")
folder("${JOB_FOLDER}/sstream-cache")
for (project in Json.projects) {
    pipelineJob("${JOB_FOLDER}/${project.name}/${project.name}") {
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '4'
            }
        }
        triggers {
            gerritTrigger {
                silentMode(false)
                serverName('ATT-airship-CI')
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
                        excludeDrafts(false)
                        excludeTrivialRebase(false)
                        excludeNoCodeChange(false)
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

base_path = "gates"
folder("${base_path}")

import groovy.json.JsonSlurper

def imagesJson = '''{ "gates":[
                          "pegleg-lint"
                        ]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.gates) {
    pipelineJob("${base_path}/${entry}") {
        displayName('Pegleg Lint')
        description('This pipeline will perform a Pegleg Lint on all site yamls')

        concurrentBuild(false)

        triggers {
            gerritTrigger {
                serverName('mtn5-jenkins')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("aic-clcp-manifests")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                    gerritProject {
                        compareType('PLAIN')
                        pattern("aic-clcp-site-manifests")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
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
                    script(readFileFromWorkspace("${base_path}/${entry}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}
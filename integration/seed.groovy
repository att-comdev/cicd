base_path = "integration"

import groovy.json.JsonSlurper

def imagesJson = '''{ "genesis":[
                          "genesis-full",
                          "site-update"
                        ]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.genesis) {
    pipelineJob("${base_path}/${entry}") {
        parameters {
            booleanParam {
                defaultValue(false)
                description('Enable Sonobuoy conformance tests')
                name ('SONOBUOY_ENABLED')
            }
        }

        concurrentBuild(false)

        triggers {
            gerritTrigger {
                serverName('Gerrithub-jenkins')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/treasuremap")
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

import groovy.json.JsonSlurper

def imagesJson = '''{ "os":[{
                        "repo":"openstack",
                        "images":[
                                  "ranger-agent",
                                  "ranger"
                                  ]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)
folder("images")
folder("images/openstack")

for (entry in object.os) {
    for (image in entry.images) {
      folder("images/openstack")
      pipelineJob("images/${entry.repo}/${image}") {
            triggers {
                gerritTrigger {
                    serverName('OS-CommunityGerrit')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("${entry.repo}/${image}")
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
                    }
                }

                definition {
                    cps {
                      script(readFileFromWorkspace("images/${entry.repo}/ranger/Jenkinsfile"))
                      sandbox(false)
                    }
                }
            }
        }
    }
}

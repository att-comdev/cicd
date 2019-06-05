import groovy.json.JsonSlurper

def imagesJson = '''{ "os":[{
                        "repo":"x/ranger",
                        "images":[
                                  "ranger",
                                  "rangercli"
                                  ]
                        },{
                        "repo":"x/ranger-agent",
                        "images":["ranger-agent"]}]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)
folder("images")
folder("images/openstack")
folder("images/openstack/ranger")

for (entry in object.os) {
    for (image in entry.images) {
      pipelineJob("images/openstack/ranger/${image}") {
            logRotator{
              daysToKeep(90)
            }
            triggers {
                gerritTrigger {
                    serverName('OS-CommunityGerrit')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("${entry.repo}")
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
                      script(readFileFromWorkspace("images/openstack/ranger/Jenkinsfile"))
                      sandbox(false)
                    }
                }
            }
        }
    }
}

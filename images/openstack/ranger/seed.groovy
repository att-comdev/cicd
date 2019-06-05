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
            parameters {
                stringParam("REPO_NAME", entry.repo, "Upstream repository")
                choiceParam('GERRIT_EVENT_TYPE',
                            ['patchset-created', 'replace-latest'],
                            'Never choose replace-latest unless you have explicit permission to do so')
                stringParam("GERRIT_PATCHSET_REVISION", "", "Patchset or latest master Commit ID")
                stringParam("GERRIT_CHANGE_ID", "", "Change ID of patchset or latest merge")
                stringParam("GERRIT_CHANGE_URL", "review.opendev.org/x/ranger", "Link to the patchset or to the upstream repo this job is run for")
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

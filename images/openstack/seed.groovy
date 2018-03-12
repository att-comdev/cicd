import groovy.json.JsonSlurper

def imagesJson = '''{ "openstackhelm":[{
                        "repo":"openstack",
                        "images":[
                                  "ceph-config-helper",
                                  "vbmc",
                                  "gate-utils",
                                  "openvswitch",
                                  "libvirt"
                                  ]
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.openstackhelm) {
    for (image in entry.images) {
      pipelineJob("images/${entry.repo}/${image}/${image}") {
            triggers {
                gerritTrigger {
                    serverName('OS-CommunityGerrit')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("${entry.repo}/openstack-helm")
                            branches {
                                branch {
                                compareType("ANT")
                                pattern("**")
                                }
                            }
                            filePaths {
                                filePath {
                                   compareType("ANT")
                                  pattern("tools/images/${image}/**")
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
                      script(readFileFromWorkspace("images/${entry.repo}/tools/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}
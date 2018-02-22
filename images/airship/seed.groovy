import groovy.json.JsonSlurper

folder("images/airship/master")
folder("images/airship/patchset")
folder("images/airship/master/airship-maas")
folder("images/airship/patchset/airship-maas")

def imagesJson = '''{ "github":[{
                               "repo":"https://git.openstack.org/openstack/airship-shipyard",
                               "directory":"images/airship/master/airship-shipyard",
                               "image":"shipyard",
                            },{
                               "repo":"https://git.openstack.org/openstack/airship-shipyard",
                                "directory":"images/airship/master/airship-shipyard/airflow",
                                "image":"airflow"
                            },{
                               "repo":"https://git.openstack.org/openstack/airship-drydock",
                                "directory":"images/airship/master/airship-drydock",
                                "image":"drydock"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/master/airship-maas/sstream-cache",
                                "image":"sstream-cache"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/master/airship-maas/maas-region-controller",
                                "image":"maas-region-controller"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/master/airship-maas/maas-rack-controller",
                                "image":"maas-rack-controller"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-deckhand",
                                "directory":"images/airship/master/airship-deckhand",
                                "image":"deckhand"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-armada",
                                "directory":"images/airship/master/airship-armada",
                                "image":"armada"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-pegleg",
                                "directory":"images/airship/master/airship-pegleg",
                                "image":"armada"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-promenade",
                                "directory":"images/airship/master/airship-promenade",
                                "image":"promenade"
                            }]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
  folder("${entry.directory}")
    pipelineJob("${entry.directory}/${entry.image}") {
        parameters {
            stringParam {
                defaultValue("${entry.repo}")
                description('Name of repo in airship to build')
                name ('GIT_REPO')
            }
            stringParam {
                defaultValue("0.9.0")
                description('Put RC version here')
                name('VERSION')
            }
            stringParam {
                defaultValue("*/master")
                description('Put Branch or commit id here')
                name('COMMIT_ID')
            }
        }
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '10'
            }
        }
        triggers {
            gerritTrigger {
                serverName('ATT-airship-CI')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/airship-${entry.image}")
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
                    changeMerged()
                    commentAddedContains {
                        commentAddedCommentContains('recheck')
                    }
                }
           }

           definition {
               cps {
                   script(readFileFromWorkspace("images/airship/JenkinsfileMaster"))
                   sandbox()
               }
           }
        }
    }
}

imagesJson = '''{ "github":[{
                               "repo":"https://git.openstack.org/openstack/airship-shipyard",
                               "directory":"images/airship/patchset/airship-shipyard",
                               "image":"shipyard"
                            },{
                               "repo":"https://git.openstack.org/openstack/airship-shipyard",
                                "directory":"images/airship/master/airship-shipyard/airflow",
                                "image":"airflow"
                            },{
                               "repo":"https://git.openstack.org/openstack/airship-drydock",
                                "directory":"images/airship/patchset/airship-drydock",
                                "image":"drydock"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/patchset/airship-maas/sstream-cache",
                                "image":"sstream-cache"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/patchset/airship-maas/maas-region-controller",
                                "image":"maas-region-controller"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/patchset/airship-maas/maas-rack-controller",
                                "image":"maas-rack-controller"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-deckhand",
                                "directory":"images/airship/patchset/airship-deckhand",
                                "image":"deckhand"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-armada",
                                "directory":"images/airship/patchset/airship-armada",
                                "image":"armada"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-pegleg",
                                "directory":"images/airship/patchset/airship-pegleg",
                                "image":"armada"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-promenade",
                                "directory":"images/airship/patchset/airship-promenade",
                                "image":"promenade"
                            }]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
  folder("${entry.directory}")
    pipelineJob("${entry.directory}/${entry.image}") {
        parameters {
            stringParam {
                defaultValue("${entry.repo}")
                description('Name of repo in airship to build')
                name ('GIT_REPO')
            }
            stringParam {
                defaultValue("0.9.0")
                description('Put RC version here')
                name('VERSION')
            }
            stringParam {
                defaultValue("*/master")
                description('Put Branch or commit id here')
                name('COMMIT_ID')
            }
        }
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '10'
            }
        }
        triggers {
            gerritTrigger {
                serverName('ATT-airship-CI')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/airship-${entry.image}")
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
                    commentAddedContains {
                        commentAddedCommentContains('recheck')
                    }
                }
           }

           definition {
               cps {
                   script(readFileFromWorkspace("images/airship/JenkinsfileMaster"))
                   sandbox()
               }
           }
        }
    }
}




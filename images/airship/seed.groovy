import groovy.json.JsonSlurper

folder("images/airship/master")
folder("images/airship/patchset")
folder("images/airship/master/airship-maas")
folder("images/airship/patchset/airship-maas")
folder("images/airship/master/airship-shipyard")
folder("images/airship/patchset/airship-shipyard")

def imagesJson = '''{ "github":[{
                               "repo":"https://git.openstack.org/openstack/airship-shipyard",
                               "directory":"images/airship/master/airship-shipyard/shipyard",
                               "image":"shipyard",
                               "name":"shipyard"
                            },{
                               "repo":"https://git.openstack.org/openstack/airship-shipyard",
                                "directory":"images/airship/master/airship-shipyard/airflow",
                                "image":"airflow",
                                "name":"shipyard"
                            },{
                               "repo":"https://git.openstack.org/openstack/airship-drydock",
                                "directory":"images/airship/master/airship-drydock",
                                "image":"drydock",
                                "name":"drydock"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/master/airship-maas/sstream-cache",
                                "image":"sstream-cache",
                                "name":"maas"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/master/airship-maas/maas-region-controller",
                                "image":"maas-region-controller",
                                "name":"maas"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/master/airship-maas/maas-rack-controller",
                                "image":"maas-rack-controller",
                                "name":"maas"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-deckhand",
                                "directory":"images/airship/master/airship-deckhand",
                                "image":"deckhand",
                                "name":"deckhand"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-armada",
                                "directory":"images/airship/master/airship-armada",
                                "image":"armada",
                                "name":"armada"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-pegleg",
                                "directory":"images/airship/master/airship-pegleg",
                                "image":"pegleg",
                                "name":"pegleg"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-promenade",
                                "directory":"images/airship/master/airship-promenade",
                                "image":"promenade",
                                "name":"promenade"
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
        }
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '10'
            }
        }
        triggers {
            gerritTrigger {
                silentMode(true)
                serverName('ATT-airship-CI')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/airship-${entry.name}")
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
                               "directory":"images/airship/patchset/airship-shipyard/shipyard",
                               "image":"shipyard",
                               "name":"shipyard",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                               "repo":"https://git.openstack.org/openstack/airship-shipyard",
                                "directory":"images/airship/patchset/airship-shipyard/airflow",
                                "image":"airflow",
                                "name":"shipyard",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                               "repo":"https://git.openstack.org/openstack/airship-drydock",
                                "directory":"images/airship/patchset/airship-drydock",
                                "image":"drydock",
                                "name":"drydock",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/patchset/airship-maas/sstream-cache",
                                "image":"sstream-cache",
                                "name":"maas",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/patchset/airship-maas/maas-region-controller",
                                "image":"maas-region-controller",
                                "name":"maas",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-maas",
                                "directory":"images/airship/patchset/airship-maas/maas-rack-controller",
                                "image":"maas-rack-controller",
                                "name":"maas",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-deckhand",
                                "directory":"images/airship/patchset/airship-deckhand",
                                "image":"deckhand",
                                "name":"deckhand",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-armada",
                                "directory":"images/airship/patchset/airship-armada",
                                "image":"armada",
                                "name":"armada",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-pegleg",
                                "directory":"images/airship/patchset/airship-pegleg",
                                "image":"pegleg",
                                "name":"pegleg",
                                "jenkinsfile_loc":"JenkinsfileMaster"
                            },{
                                "repo":"https://git.openstack.org/openstack/airship-promenade",
                                "directory":"images/airship/patchset/airship-promenade",
                                "image":"multi-node-promenade",
                                "name":"promenade",
                                "jenkinsfile_loc":"JenkinsfilePromenade"
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
        }
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '10'
            }
        }
        triggers {
            gerritTrigger {
                silentMode(true)
                serverName('ATT-airship-CI')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/airship-${entry.name}")
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
                 script(readFileFromWorkspace("images/airship/${entry.jenkinsfile_loc}"))
                   sandbox()
               }
           }
        }
    }
}




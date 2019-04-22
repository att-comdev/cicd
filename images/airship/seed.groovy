import groovy.json.JsonSlurper
//This will change once all images/airship/xxx are deprecated
folder("images")
folder("images/airship")
folder("images/airship/update")
folder("images/airship/update/shipyard")
folder("images/airship/update/maas")
folder("images/airship/patchset")
def imagesJson = '''{ "github":[{
                               "repo":"https://opendev.org/airship/shipyard",
                               "directory":"images/airship/update/shipyard/shipyard",
                               "image":"shipyard",
                               "name":"shipyard"
                            },{
                               "repo":"https://opendev.org/airship/shipyard",
                                "directory":"images/airship/update/shipyard/airflow",
                                "image":"airflow",
                                "name":"shipyard"
                            },{
                               "repo":"https://opendev.org/airship/drydock",
                                "directory":"images/airship/update/drydock",
                                "image":"drydock",
                                "name":"drydock"
                            },{
                                "repo":"https://opendev.org/airship/maas",
                                "directory":"images/airship/update/maas/maas-region-controller",
                                "image":"maas-region-controller",
                                "name":"maas"
                            },{
                                "repo":"https://opendev.org/airship/maas",
                                "directory":"images/airship/update/maas/maas-rack-controller",
                                "image":"maas-rack-controller",
                                "name":"maas"
                            },{
                                "repo":"https://opendev.org/airship/maas",
                                "directory":"images/airship/update/maas/sstream-cache",
                                "image":"sstream-cache",
                                "name":"maas"
                            },{
                                "repo":"https://opendev.org/airship/deckhand",
                                "directory":"images/airship/update/deckhand",
                                "image":"deckhand",
                                "name":"deckhand"
                            },{
                                "repo":"https://opendev.org/airship/armada",
                                "directory":"images/airship/update/armada",
                                "image":"armada",
                                "name":"armada"
                            },{
                                "repo":"https://opendev.org/airship/pegleg",
                                "directory":"images/airship/update/pegleg",
                                "image":"pegleg",
                                "name":"pegleg"
                            },{
                                "repo":"https://opendev.org/airship/promenade",
                                "directory":"images/airship/update/promenade",
                                "image":"promenade",
                                "name":"promenade"
                            }]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
  folder("${entry.directory}")
    pipelineJob("${entry.directory}/${entry.image}") {
        logRotator{
            daysToKeep(90)
        }
        parameters {
            stringParam {
                defaultValue("${entry.repo}")
                description('Name of repo in airship to build')
                name ('GIT_REPO')
                trim(true)
            }
            stringParam {
                defaultValue("1.0.0")
                description('Put RC version here')
                name('VERSION')
                trim(true)
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
                        pattern("airship/${entry.name}")
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
                   script(readFileFromWorkspace("images/airship/JenkinsfileMaster"))
                   sandbox(false)
               }
           }
        }
    }
}

imagesJson = '''{ "github":[{
                                "repo":"https://opendev.org/airship/promenade",
                                "directory":"images/airship/patchset/promenade",
                                "image":"multi-node-promenade",
                                "name":"promenade",
                                "jenkinsfile_loc":"JenkinsfilePromenade"
                            }]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
  folder("${entry.directory}")
    pipelineJob("${entry.directory}/${entry.image}") {
        logRotator{
            daysToKeep(90)
        }
        parameters {
            stringParam {
                defaultValue("${entry.repo}")
                description('Name of repo in airship to build')
                name ('GIT_REPO')
                trim(true)
            }
            stringParam {
                defaultValue("1.0.0")
                description('Put RC version here')
                name('VERSION')
                trim(true)
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
                        pattern("airship/${entry.name}")
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
                   sandbox(false)
               }
           }
        }
    }
}

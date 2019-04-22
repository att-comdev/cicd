import groovy.json.JsonSlurper
//This will change once all images/airship/xxx are deprecated
folder("images/airship/mtn29/shipyard")
folder("images/airship/mtn29/maas")
folder("images/airship/patchset")
def imagesJson = '''{ "github":[{
                               "repo":"https://opendev.org/airship/shipyard",
                               "directory":"images/airship/mtn29/shipyard/shipyard",
                               "image":"shipyard",
                               "name":"shipyard"
                            },{
                               "repo":"https://opendev.org/airship/shipyard",
                                "directory":"images/airship/mtn29/shipyard/airflow",
                                "image":"airflow",
                                "name":"shipyard"
                            },{
                               "repo":"https://opendev.org/airship/drydock",
                                "directory":"images/airship/mtn29/drydock",
                                "image":"drydock",
                                "name":"drydock"
                            },{
                                "repo":"https://opendev.org/airship/maas",
                                "directory":"images/airship/mtn29/maas/maas-region-controller",
                                "image":"maas-region-controller",
                                "name":"maas"
                            },{
                                "repo":"https://opendev.org/airship/maas",
                                "directory":"images/airship/mtn29/maas/maas-rack-controller",
                                "image":"maas-rack-controller",
                                "name":"maas"
                            },{
                                "repo":"https://opendev.org/airship/maas",
                                "directory":"images/airship/mtn29/maas/sstream-cache",
                                "image":"sstream-cache",
                                "name":"maas"
                            },{
                                "repo":"https://opendev.org/airship/deckhand",
                                "directory":"images/airship/mtn29/deckhand",
                                "image":"deckhand",
                                "name":"deckhand"
                            },{
                                "repo":"https://opendev.org/airship/armada",
                                "directory":"images/airship/mtn29/armada",
                                "image":"armada",
                                "name":"armada"
                            },{
                                "repo":"https://opendev.org/airship/pegleg",
                                "directory":"images/airship/mtn29/pegleg",
                                "image":"pegleg",
                                "name":"pegleg"
                            },{
                                "repo":"https://opendev.org/airship/promenade",
                                "directory":"images/airship/mtn29/promenade",
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
            }
            stringParam {
                defaultValue("")
                description('Only for manual builds')
                name ('GERRIT_PATCHSET_REVISION')
            }
            stringParam {
                defaultValue("")
                description('Only for manual builds')
                name ('GERRIT_NEWREV')
            }
            stringParam {
                defaultValue("")
                description('Only for manual builds')
                name ('GERRIT_CHANGE_URL')
            }
            stringParam {
                defaultValue("")
                description('Only for manual builds')
                name ('GERRIT_CHANGE_EVENT')
            }
            stringParam {
                defaultValue("")
                description('Only for manual builds')
                name ('GERRIT_REFSPEC')
            }
            stringParam {
                defaultValue("1.0.0")
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
                    changeMerged()
                    commentAddedContains {
                        commentAddedCommentContains('recheck')
                    }
                }
           }

           definition {
               cps {
                   script(readFileFromWorkspace("images/airship/Jenkinsfile-mtn29"))
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
            }
            stringParam {
                defaultValue("1.0.0")
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

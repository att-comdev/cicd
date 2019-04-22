import groovy.json.JsonSlurper
//This will change once all images/airship/xxx are deprecated
folder("images")
folder("images/airship")
basefolder="images/airship"

def imagesJson = '''{ "github":[{
                               "repo":"https://review.opendev.org/airship/shipyard",
                               "directory":"${basefolder}",
                               "image":"shipyard",
                               "name":"shipyard"
                            },{
                               "repo":"https://review.opendev.org/airship/shipyard",
                                "directory":"${basefolder}",
                                "image":"airflow",
                                "name":"shipyard"
                            },{
                               "repo":"https://review.opendev.org/airship/drydock",
                                "directory":"${basefolder}",
                                "image":"drydock",
                                "name":"drydock"
                            },{
                                "repo":"https://review.opendev.org/airship/maas",
                                "directory":"${basefolder}",
                                "image":"maas-region-controller",
                                "name":"maas"
                            },{
                                "repo":"https://review.opendev.org/airship/maas",
                                "directory":"${basefolder}",
                                "image":"maas-rack-controller",
                                "name":"maas"
                            },{
                                "repo":"https://review.opendev.org/airship/maas",
                                "directory":"${basefolder}",
                                "image":"sstream-cache",
                                "name":"maas"
                            },{
                                "repo":"https://review.opendev.org/airship/deckhand",
                                "directory":"${basefolder}",
                                "image":"deckhand",
                                "name":"deckhand"
                            },{
                                "repo":"https://review.opendev.org/airship/armada",
                                "directory":"${basefolder}",
                                "image":"armada",
                                "name":"armada"
                            },{
                                "repo":"https://review.opendev.org/airship/pegleg",
                                "directory":"${basefolder}",
                                "image":"pegleg",
                                "name":"pegleg"
                            },{
                                "repo":"https://review.opendev.org/airship/promenade",
                                "directory":"${basefolder}",
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

//TODO(sh8121) this should be moved to something outside of images as it is not a build
//             pipeline. also it currently fails always.
//imagesJson = '''{ "github":[{
//                                "repo":"https://review.opendev.org/airship/promenade",
//                                "directory":"images/airship/patchset/promenade",
//                                "image":"multi-node-promenade",
//                                "name":"promenade",
//                                "jenkinsfile_loc":"JenkinsfilePromenade"
//                            }]}'''
//
//jsonSlurper = new JsonSlurper()
//object = jsonSlurper.parseText(imagesJson)
//
//for (entry in object.github) {
//  folder("${entry.directory}")
//    pipelineJob("${entry.directory}/${entry.image}") {
//        logRotator{
//            daysToKeep(90)
//        }
//        parameters {
//            stringParam {
//                defaultValue("${entry.repo}")
//                description('Name of repo in airship to build')
//                name ('GIT_REPO')
//                trim(true)
//            }
//            stringParam {
//                defaultValue("1.0.0")
//                description('Put RC version here')
//                name('VERSION')
//                trim(true)
//            }
//        }
//        configure {
//            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
//                durationName 'hour'
//                count '10'
//            }
//        }
//        triggers {
//            gerritTrigger {
//                silentMode(true)
//                serverName('ATT-airship-CI')
//                gerritProjects {
//                    gerritProject {
//                        compareType('PLAIN')
//                        pattern("airship/${entry.name}")
//                        branches {
//                            branch {
//                                compareType("ANT")
//                                pattern("**")
//                            }
//                        }
//                        disableStrictForbiddenFileVerification(false)
//                    }
//                }
//                triggerOnEvents {
//                    patchsetCreated {
//                       excludeDrafts(false)
//                       excludeTrivialRebase(false)
//                       excludeNoCodeChange(false)
//                    }
//                    commentAddedContains {
//                        commentAddedCommentContains('recheck')
//                    }
//                }
//           }
//
//           definition {
//               cps {
//                 script(readFileFromWorkspace("images/airship/${entry.jenkinsfile_loc}"))
//                   sandbox(false)
//               }
//           }
//        }
//    }
//}

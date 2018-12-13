import groovy.json.JsonSlurper

//This will change once all images/airship/xxx are deprecated
def imagesJson = '''{ "github":[{
                                "repo":"https://git.openstack.org/openstack/airship-pegleg",
                                "directory":"utils",
                                "image":"pegleg",
                                "name":"pegleg"
                            }]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
    folder("${entry.directory}")
    pipelineJob("${entry.directory}/${entry.image}") {
        logRotator {
            daysToKeep(90)
        }
        parameters {
            stringParam {
                description('Name of site')
                name('PROJECT_SITE')
            }
        }
        configure {
            node ->
                node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl' {
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
                    changeMerged()
                    commentAddedContains {
                        commentAddedCommentContains('recheck')
                    }
                }
            }

            definition {
                cps {
                    script(readFileFromWorkspace("utils/pegleg/Jenkinsfile"))
                    sandbox(false)
                }
            }
        }
    }
}
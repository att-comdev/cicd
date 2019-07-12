import groovy.json.JsonSlurper
folder("images")
folder("images/airship")
folder("images/airship/patchset")

imagesJson = '''{ "github":[{
                                "repo":"https://review.opendev.org/airship/promenade",
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

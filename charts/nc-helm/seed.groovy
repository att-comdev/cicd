import groovy.json.JsonSlurper

folder("charts/nc-helm")
folder("charts/openstack")

def imagesJson = '''{ "github":[{
                               "repo": "\${GERRIT_SCHEME}://\${GERRIT_HOST}:\${GERRIT_PORT}",
                               "directory": "charts/nc-helm",
                               "project": "nc-helm",
                               "trigger": "mtn5-gerrit"
                            },
                               "repo": "\${GERRIT_SCHEME}://\${GERRIT_HOST}:\${GERRIT_PORT}",
                               "directory": "charts/openstack/openstack-helm",
                               "project": "openstack/openstack-helm",
                               "trigger": "OS-CommunityGerrit
                            }
                            ]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
  folder("${entry.directory}")
    pipelineJob("${entry.directory}") {
        parameters {
            stringParam {
                defaultValue("${entry.repo}/${entry.project}")
                description('Name of repo in openstack to build')
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
                serverName("${entry.trigger}")
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("${entry.project}")
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
                   script(readFileFromWorkspace("charts/nc-helm/Jenkinsfile"))
                   sandbox()
               }
           }
        }
    }
}

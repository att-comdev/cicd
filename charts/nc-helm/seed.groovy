import groovy.json.JsonSlurper

folder("charts/nc-helm")
folder("charts/openstack")
folder("charts/airship")

def imagesJson = '''{ "github":[{
                               "repo": "$/{GERRIT_SCHEME/}:////$/{GERRIT_HOST/}:$/{GERRIT_PORT/}",
                               "directory": "charts/nc-helm",
                               "gerrit_project": "nc-helm",
                               "trigger": "mtn5-gerrit",
                               "project_name":"nc_helm"
                            },{
                               "repo": "$/{GERRIT_SCHEME/}:////$/{GERRIT_HOST/}:$/{GERRIT_PORT/}",
                               "directory": "charts/openstack",
                               "gerrit_project": "openstack/openstack-helm",
                               "trigger": "OS-CommunityGerrit",
                               "project_name":"openstack-helm"
                            },
                            {
                               "repo": "$/{GERRIT_SCHEME/}:////$/{GERRIT_HOST/}:$/{GERRIT_PORT/}",
                               "directory": "charts/airship",
                               "gerrit_project": "openstack/airship-deckhand",
                               "trigger": "ATT-airship-CI",
                               "project_name":"airship-deckhand"
                            }
                            ]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
  folder("${entry.directory}")
  pipelineJob("${entry.directory}/${entry.project_name}") {
        parameters {
            stringParam {
                defaultValue("${entry.repo}/${entry.gerrit_project}")
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
                        pattern("${entry.gerrit_project}")
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

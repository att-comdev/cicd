import groovy.json.JsonSlurper

def chartsJson = '''{ "airship":[{
                        "repo_name":"airship-maas",
                        "git_repo":"airship/maas",
                        "ref":"master"
                    }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

for (entry in object.airship) {
    pipelineJob("charts/airship/${entry.repo_name}") {
        disabled(false)
        logRotator{
            daysToKeep(90)
        }
        parameters {
            stringParam('MAAS_PROJECT',"${entry.git_repo}")
            stringParam('INFRA_PROJECT', "openstack/openstack-helm-infra")
            stringParam('OPENSTACK_HELM_INFRA_COMMIT', "master")
        }
        triggers {
            gerritTrigger {
                silentMode(true)
                serverName('OS-CommunityGerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("${entry.git_repo}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/openstack-helm-infra")
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
                }
            }
            definition {
                cps {
                    script(readFileFromWorkspace('charts/airship/airship-maas/Jenkinsfile'))
                    sandbox(false)
                }
            }
        }
    }
}

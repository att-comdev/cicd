
import groovy.json.JsonSlurper

def chartsJson = '''{ "airship":[{
                        "repo_name":"airship-divingbell",
                        "git_repo":"airship/divingbell",
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
            stringParam('DIVINGBELL_PROJECT',"${entry.git_repo}")
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
                    script(readFileFromWorkspace('charts/airship/airship-divingbell/Jenkinsfile'))
                    sandbox(false)
                }
            }
        }
    }
}

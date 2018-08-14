import groovy.json.JsonSlurper

def chartsJson = '''{ "airship":[{
                        "repo_name":"airship-drydock",
			"git_repo":"openstack/airship-drydock",
			"project":"drydock",
			"ref":"master"
                    }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

for (entry in object.airship) {
        pipelineJob("charts/airship/${entry.repo_name}") {
            disabled(false)
            parameters {
                stringParam('REPO_NAME',"${entry.repo_name}")
                stringParam('PROJECT',"${entry.git_repo}")
            }
            triggers {
                gerritTrigger {
                    silentMode(true)
                    serverName('ATT-airship-CI')
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
                                pattern("**/helm-toolkit")
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
                        script(readFileFromWorkspace('charts/airship/Jenkinsfile'))
                        sandbox()
                    }
                }
            }
        }
    }

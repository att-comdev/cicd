
pipelineJob("UCP/Deckhand/deckhand") {

    parameters {
        stringParam {
            defaultValue(GERRIT_REFSPEC)
            description('Pass att-comdev/cicd code refspec to the job')
            name ('CICD_GERRIT_REFSPEC')
        }
    }

    triggers {
        gerritTrigger {
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("att-comdev/deckhand")
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
            }
        }

        definition {
            cps {
                script(readFileFromWorkspace("ucp/deckhand/Jenkinsfile"))
                sandbox()
            }
        }
    }
}


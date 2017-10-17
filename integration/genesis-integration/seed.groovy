base_path = "integration/genesis-integration"

pipelineJob("${base_path}/genesis-full") {

    parameters {
        stringParam {
            defaultValue(GERRIT_REFSPEC)
            description('Pass att-comdev/cicd code refspec to the job')
            name ('CICD_GERRIT_REFSPEC')
        }
        stringParam {
            defaultValue('refs/changes/46/46/89')
            description('Pass att-comdev/cicd code refspec to the job')
            name ('CLCP_INTEGRATION_REFSPEC')
        }
        booleanParam {
            defaultValue(false)
            description('Enable Sonobuoy conformance tests')
            name ('SONOBUOY_ENABLED')
        }
    }

    triggers {
        gerritTrigger {
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("att-comdev/ucp-integration")
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
                script(readFileFromWorkspace("${base_path}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}


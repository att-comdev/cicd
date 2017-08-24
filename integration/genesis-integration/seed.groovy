
job_path = "integration/genesis-integration"

pipelineJob(job_path) {

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
            patchsetCreated {
               excludeDrafts(false)
               excludeTrivialRebase(false)
               excludeNoCodeChange(false)
            }
            changeMerged()
        }

        definition {
            cps {
                script(readFileFromWorkspace("${job_path}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}


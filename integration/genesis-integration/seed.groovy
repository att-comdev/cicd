
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
                    filePaths {
                        filePath {
                            compareType("ANT")
                            pattern("${job_path}/**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                changeMerged()
                patchsetCreated()
            }
        }

        definition {
            cps {
                script(readFileFromWorkspace("${job_path}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}


JOB_FOLDER='integration'
JOB_NAME='site-update'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        booleanParam {
            defaultValue(false)
            description('Enable Sonobuoy conformance tests')
            name ('SONOBUOY_ENABLED')
        }
    }

    concurrentBuild(false)

    blockOn('genesis-full')
    triggers {
        upstream('genesis-full', 'SUCCESS')
        gerritTrigger {
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("att-comdev/treasuremap")
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
                script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
                sandbox(false)
            }
        }
    }
}

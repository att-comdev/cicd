JOB_BASE = 'integration/airship'
folder('integration/airship')

pipelineJob("${JOB_BASE}/airship-multinode") {

    logRotator{
        daysToKeep(90)
    }

    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '3'
        }
    }

    triggers {
        gerritTrigger {
            serverName('ATT-airship-CI')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("airship/in-a-bottle")
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
                   commentAddedCommentContains('^recheck\$')
                }
            }
        }

        definition {
            cps {
                script(readFileFromWorkspace("${JOB_BASE}/gate-multinode/Jenkinsfile"))
                sandbox(false)
            }
        }
    }
}

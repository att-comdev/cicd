
JOB_BASE = 'charts/openstack/openstack-helm/single-dev'

pipelineJob(JOB_BASE) {

    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '300'
        }
    }

    parameters {
        booleanParam {
            defaultValue(false)
            description('Keep deployment running for 1h after success')
            name ('DELAY_DESTROY')
        }
    }

    triggers {
        gerritTrigger {
            serverName('OS-CommunityGerrit')
            silentMode(true)

            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("openstack/openstack-helm")
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
                script(readFileFromWorkspace("${JOB_BASE}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}


JOB_FOLDER="images/airship/shipyard/airflow"
folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/airflow") {
    configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '3'
                }
    }
    triggers {
        gerritTrigger {
            silentMode(false)
            serverName('ATT-airship-CI')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("openstack/airship-shipyard")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(true)
                    excludeTrivialRebase(false)
                    excludeNoCodeChange(true)
                }
                changeMerged()
                commentAddedContains {
                   commentAddedCommentContains('recheck')
                }
            }
        }
    }
    definition {
        cps {
          script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
            sandbox()
        }
    }
}

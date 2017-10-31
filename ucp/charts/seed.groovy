pipelineJob("ucp/charts/all") {
    triggers {
        gerritTrigger {
            serverName('OS-CommunityGerrit')
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
                    filePaths {
                        filePath {
                            compareType(REG_EXP)
                            pattern('helm-toolkit/.*')
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                changeMerged()
                patchsetCreated {
                   excludeDrafts(true)
                   excludeTrivialRebase(true)
                   excludeNoCodeChange(true)
                }
            }
        }
        definition {
            cps {
                script(readFileFromWorkspace('ucp/charts/Jenkinsfile'))
                sandbox()
            }
        }
    }
}

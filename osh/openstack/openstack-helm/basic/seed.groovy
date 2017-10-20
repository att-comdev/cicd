pipelineJob("charts/openstack/openstack-helm/basic") {

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
                script(readFileFromWorkspace("osh/openstack/openstack-helm/basic/Jenkinsfile"))
                sandbox()
            }
        }
    }
}





JOB_BASE='images/openstack/loci'
folder("${JOB_BASE}")

// todo: think if we shall move reqs out from the loop
// reqs potentially may have different triggers/lifecycle
PROJECTS = ['requirements', 'keystone', 'heat']

// todo: work on project list.. not all ready yet
// PROJECTS = ['requirements', 'keystone', 'heat', 'cinder', 'glance', 'horizon', 'neutron', 'nova', 'ironic']


PROJECTS.each {
    def project = it
    pipelineJob("${JOB_BASE}/${project}") {

        // limit surge of patchsets
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '3'
            }
        }

        triggers {
            gerritTrigger {
                serverName('OS-CommunityGerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/${project}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                }

                // todo: figure out if we can trigger on tag update
                // most projects are now tagged (not branches)
                // refs not used right now.. way to get some builds now and then
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
}

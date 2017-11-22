
JOB_BASE='images/openstack/loci'
folder("${JOB_BASE}")

// todo: think if we shall move reqs out from the loop
// reqs potentially may have different triggers/lifecycle
// { project: 'default-ref }
PROJECTS = ['requirements': 'stable/newton',
            'keystone': 'newton-eol', 
            'heat': 'newton-eol']

// todo: work on project list.. not all ready yet
// PROJECTS = ['requirements', 'keystone', 'heat', 'cinder', 'glance', 'horizon', 'neutron', 'nova', 'ironic']


PROJECTS.each { project, ref ->

    pipelineJob("${JOB_BASE}/${project}") {

        // limit surge of patchsets
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '3'
            }
        }

        parameters {
            stringParam {
                defaultValue(ref)
                description('Default')
                name ('PROJECT_REF')
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
                                pattern("newton-eol")
                            }
                            branch {
                                compareType("ANT")
                                pattern("stable/newton")
                            }
                            branch {
                                compareType("ANT")
                                pattern("stable/ocata")
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


// use case of building a version
// use case of triggering all the tags


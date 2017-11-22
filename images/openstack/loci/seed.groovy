
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

                // todo: look into triggers more
                triggerOnEvents {
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

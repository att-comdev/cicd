
JOB_BASE='images/openstack/loci'
folder("${JOB_BASE}/community")

// { project: 'ref' }
PROJECTS = ['requirements': 'stable/newton',
            'keystone': 'newton-eol',
            'heat': 'newton-eol']
//            'cinder': 'newton-eol',
//            'glance': 'newton-eol',
//            'horizon': 'newton-eol',
//            'nova': 'stable/newton',
//            'ironic': 'stable/newton']


PROJECTS.each { project, ref ->

    pipelineJob("${JOB_BASE}/community/${project}") {

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
                description('Default branch for manual build.\n\n' +
                            'Currently master, stable/<branch>, and newton-eol are supported')
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
                                pattern("stable/*")
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
}


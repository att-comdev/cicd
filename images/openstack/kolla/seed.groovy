JOB_BASE='images/openstack/kolla'

folder("${JOB_BASE}/community")
folder("${JOB_BASE}/mos")

COMMUNITY_PROJECTS = ['neutron': 'newton-eol',
            'keystone': 'newton-eol',
            'heat': 'newton-eol',
            'cinder': 'newton-eol',
            'glance': 'newton-eol',
            'horizon': 'newton-eol',
            'nova': 'stable/newton',
            'barbican': 'newton-eol',
            'ceilometer': 'newton-eol',
            'rally': 'newton-eol',
            'mistral': 'newton-eol',
            'magnum': 'newton-eol']

MOS_PROJECTS = ['keystone': 'main/newton',
            'heat': 'main/newton',
            'cinder': 'main/newton',
            'glance': 'main/newton',
            'horizon': 'main/newton',
            'nova': 'main/newton',
            'ceilometer': 'main/newton',
            'mistral': 'main/newton',
            'neutron': 'main/newton']


COMMUNITY_PROJECTS.each { project, ref ->

    pipelineJob("${JOB_BASE}/community/${project}") {

        parameters {
            stringParam {
                defaultValue(ref)
                description('Default branch')
                name ('PROJECT_REF')
            }
        }

        triggers {
            gerritTrigger {
                serverName('OS-CommunityGerrit')
                silentMode(true)

                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/${project}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("stable/newton")
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


MOS_PROJECTS.each { project, ref ->

    pipelineJob("${JOB_BASE}/mos/${project}") {

        parameters {
            stringParam {
                defaultValue(ref)
                description('Default branch')
                name ('PROJECT_REF')
            }
        }

        triggers {
            gerritTrigger {
                serverName('internal-gerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("mos-${project}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("main/newton")
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

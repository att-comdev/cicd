
JOB_BASE='images/openstack/loci'

folder("${JOB_BASE}/community")
folder("${JOB_BASE}/mos")

// { project: 'ref' }
COMMUNITY_PROJECTS = ['requirements': 'stable/newton',
            'keystone': 'newton-eol',
            'heat': 'newton-eol',
            'glance': 'newton-eol',
            'cinder': 'newton-eol',
            'neutron': 'newton-eol',
            'nova': 'stable/newton']

MOS_PROJECTS = ['mos-keystone': 'main/newton',
                'mos-heat': 'main/newton']
//                'mos-cinder': 'main/newton',
//                'mos-glance': 'main/newton',
//                'mos-horizon': 'main/newton',
//                'mos-nova': 'main/newton',
//                'mos-ironic': 'main/newton']


COMMUNITY_PROJECTS.each { project, ref ->

    pipelineJob("${JOB_BASE}/community/${project}") {

        // limit surge of patchsets
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '300'
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
                silentMode(true)

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


MOS_PROJECTS.each { project, ref ->

    pipelineJob("${JOB_BASE}/mos/${project}") {

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
                            'Currently main/newton is supported.')
                name ('PROJECT_REF')
            }
        }

        triggers {
            gerritTrigger {
                serverName('internal-gerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern(project)
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


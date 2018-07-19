
JOB_BASE='images/openstack/loci'
folder("${JOB_BASE}/community")
folder("${JOB_BASE}/mos")
// { project: 'ref' }
COMMUNITY_PROJECTS = ['requirements': 'stable/*',
                      'keystone': 'stable/*',
                      'heat': 'stable/*',
                      'glance': 'stable/*',
                      'barbican': 'stable/*',
                      'cinder': 'stable/*',
                      'neutron': 'stable/*',
                      'nova': 'stable/*',
                      'horizon': 'stable/*']
// master is ocata branch for mos
MOS_PROJECTS = ['mos-requirements': 'master',
                'mos-keystone': 'master',
                'mos-heat': 'master',
                'mos-glance': 'master',
                'mos-cinder': 'master',
                'mos-neutron': 'master',
                'mos-nova': 'master',
                'mos-horizon': 'master',
                'mos-neutron-sriov': 'master',
                'mos-nova-1804': 'master']
COMMUNITY_PROJECTS.each { project, ref ->
    pipelineJob("${JOB_BASE}/community/${project}") {
        // limit surge of patchsets
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '10'
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
                                pattern(ref)
                            }
                            // project configs are not yet defined for master(rocky)
                            // disabling master for now
                            //branch {
                                //compareType("ANT")
                                //pattern("master")
                            //}
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
                    script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileCommunity"))
                    sandbox()
                }
            }
        }
    }
}

MOS_PROJECTS.each { project, ref ->
    pipelineJob("${JOB_BASE}/mos/${project}") {
        def propsPrefix = "${project}".split('-')[0]
        def propsSuffix = "${project}".split('-')[1]
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
                            'Currently master is supported.')
                name ('PROJECT_REF')
            }
        }
        triggers {
            gerritTrigger {
                serverName('mtn5-gerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("${propsPrefix}-${propsSuffix}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern(ref)
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
                    script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileMos"))
                    sandbox()
                }
            }
        }
    }
}

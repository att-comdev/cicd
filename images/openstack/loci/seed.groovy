
JOB_BASE='images/openstack/loci'
folder("${JOB_BASE}/community")
folder("${JOB_BASE}/mos")
LOCI_BASE_IMAGE = "${ARTF_SECURE_DOCKER_URL}/ubuntu/16.04/nc-ubuntu-16.04:2018-05-01_12-21-32"
LOCI_BASE_IMAGE_XENIAL = "${ARTF_DOCKER_URL}/ubuntu:xenial"
LOCI_SRIOV_BASE_IMAGE = "${ARTF_SECURE_DOCKER_URL}/ubuntu/18.04/nc-ubuntu-18.04:2018-05-01_05-48-21"
// { project: 'ref' }
COMMUNITY_PROJECTS = ['requirements': 'stable/ocata',
                      'keystone': 'stable/ocata',
                      'heat': 'stable/ocata',
                      'glance': 'stable/ocata',
                      'barbican': 'stable/ocata',
                      'cinder': 'stable/ocata',
                      'neutron': 'stable/ocata',
                      'nova': 'stable/ocata',
                      'horizon': 'stable/ocata']
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
                count '3'
            }
        }
        parameters {
            stringParam {
                defaultValue(ref)
                description('Default branch for manual build.\n\n' +
                            'Currently master, stable/<branch>, and newton-eol are supported')
                name ('PROJECT_REF')
                //trim(true)
            }
            stringParam {
                defaultValue("${LOCI_BASE_IMAGE_XENIAL}")
                description('Image needed for 16.04')
                name ('LOCI_BASE_IMAGE')
                //trim(true)
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
                            branch {
                                compareType("ANT")
                                pattern("master")
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
                    script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileCommunity"))
                    sandbox()
                }
            }
        }
    }
}
//temporary
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
                //trim(true)
            }
          if (project == "mos-neutron-sriov" || project == "mos-nova-1804") {
            stringParam {
                defaultValue("${LOCI_SRIOV_BASE_IMAGE}")
                description('Image needed for SR-IOV')
                name ('LOCI_BASE_IMAGE')
                //trim(true)
            }
          } else {
            stringParam {
                defaultValue("${LOCI_BASE_IMAGE}")
                description('Image needed for 16.04')
                name ('LOCI_BASE_IMAGE')
                //trim(true)
            }
          }
        }
        triggers {
            gerritTrigger {
                silentMode(true)
                serverName('mtn5-gerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("${propsPrefix}-${propsSuffix}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("master")
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

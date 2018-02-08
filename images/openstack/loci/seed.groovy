
JOB_BASE='images/openstack/loci'

folder("${JOB_BASE}/community")
folder("${JOB_BASE}/mos")
SEMANTIC_RELEASE_VERSION="0.8"
LOCI_BASE_IMAGE="${ARTF_DOCKER_URL}/ubuntu:xenial"
LOCI_SRIOV_BASE_IMAGE="${ARTF_DOCKER_URL}/ubuntu:18.04"
// { project: 'ref' }
COMMUNITY_PROJECTS = ['requirements': 'stable/ocata',
                      'keystone': 'stable/ocata',
                      'heat': 'stable/ocata',
                      'glance': 'stable/ocata',
                      'cinder': 'stable/ocata',
                      'neutron': 'stable/ocata',
                      'nova': 'stable/ocata']

// master is ocata branch for mos
MOS_PROJECTS = ['mos-keystone': 'master',
                'mos-heat': 'master',
                'mos-glance': 'master',
                'mos-cinder': 'master',
                'mos-neutron': 'master',
                'mos-nova': 'master',
                'mos-horizon': 'master']

MOS_BASELINE_PROJECTS = ['mos-keystone': 'master/ocata',
                'mos-heat': 'master/ocata',
                'mos-glance': 'master/ocata',
                'mos-cinder': 'master/ocata',
                'mos-neutron': 'master/ocata',
                'mos-nova': 'master/ocata',
                'mos-horizon': 'master/ocata',
                'mos-neutron-sriov':'master/ocata',
                'mos-nova-1804':'master/ocata']


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
            }
            stringParam {
                defaultValue("${LOCI_BASE_IMAGE}")
                description('Image needed for 16.04')
                name ('LOCI_BASE_IMAGE')
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
                serverName('mtn5-gerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern(project)
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
                    script(readFileFromWorkspace("${JOB_BASE}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}
//temporary
MOS_BASELINE_PROJECTS.each { project, ref ->

    pipelineJob("${JOB_BASE}/mos-baseline/${project}") {
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
                            'Currently main/newton is supported.')
                name ('PROJECT_REF')
            }
          if(("${project}"==("mos-neutron-sriov")) || ("${project}"==("mos-nova-1804"))){
            stringParam {
                defaultValue("${LOCI_SRIOV_BASE_IMAGE}")
                description('Image needed for SR-IOV')
                name ('LOCI_BASE_IMAGE')
            }
          } else {
            stringParam {
                defaultValue("${LOCI_BASE_IMAGE}")
                description('Image needed for 16.04')
                name ('LOCI_BASE_IMAGE')
            }
          }
          stringParam('RELEASE_CURRENT_KEY',"5EC.images.${propsPrefix}.${propsSuffix}.dev.current")
          stringParam('RELEASE_STATUS_KEY',"5EC.images.${propsPrefix}.${propsSuffix}.dev.status")
        }

        triggers {
            gerritTrigger {
                serverName('mtn5-gerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern(project)
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
                    script(readFileFromWorkspace("${JOB_BASE}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}


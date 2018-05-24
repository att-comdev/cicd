JOB_BASE='images/openstack/'
folder("${JOB_BASE}/deploy-osh")
// { project: 'ref' }
MOS_PROJECTS = ['mos-keystone': 'master',
                'mos-heat': 'master',
                'mos-horizon': 'master',
                'mos-glance': 'master',
                'mos-cinder': 'master',
                'mos-neutron': 'master',
                'mos-nova': 'master']
MOS_PROJECTS.each { project, ref ->
    pipelineJob("${JOB_BASE}/deploy-osh/${project}") {
        // limit surge of patchsets
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '3'
            }
        }
        parameters {
            stringParam {
                defaultValue()
                description('Default Keystone loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_KEYSTONE_LOCI')
            }
            stringParam {
                defaultValue()
                description('Default Heat loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_HEAT_LOCI')
            }
            stringParam {
                defaultValue()
                description('Default Horizon loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_HORIZON_LOCI')
            }
            stringParam {
                defaultValue()
                description('Default Glance loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_GLANCE_LOCI')
            }
            stringParam {
                defaultValue()
                description('Default Cinder loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_CINDER_LOCI')
            }
            stringParam {
                defaultValue()
                description('Default Nova loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_NOVA_LOCI')
            }
            stringParam {
                defaultValue()
                description('Default Nova 1804 loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_NOVA_1804_LOCI')
            }
            stringParam {
                defaultValue()
                description('Default Neutron loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_NEUTRON_LOCI')
            }
            }
            stringParam {
                defaultValue()
                description('Default Neutron Sriov loci image for build.\n\n' +
                            'Currently mos is supported.')
                name ('MOS_NEUTRON_SRIOV_LOCI')
            }
        }
        triggers {
            gerritTrigger {
                silentMode(true)
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
                    script(readFileFromWorkspace("${JOB_BASE}/deploy-osh/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}

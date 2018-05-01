
JOB_BASE='images/openstack/loci'

folder("${JOB_BASE}/mos-internal")

// { project: 'ref' }
MOS_PROJECTS = ['mos-keystone': 'master',
                'mos-heat': 'master',
                'mos-glance': 'master',
                'mos-cinder': 'master',
                'mos-neutron': 'master',
                'mos-nova': 'master']



MOS_PROJECTS.each { project, ref ->

    pipelineJob("${JOB_BASE}/mos-internal/${project}") {

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
                    script(readFileFromWorkspace("${JOB_BASE}/mos-internal/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}


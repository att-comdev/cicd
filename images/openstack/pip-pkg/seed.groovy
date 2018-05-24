
JOB_BASE='images/openstack/'

folder("${JOB_BASE}/mos")

SEMANTIC_RELEASE_VERSION="0.8"

// master is ocata branch for mos
MOS_PROJECTS = ['mos-keystoneclient': 'master',
                'mos-heatclient': 'master',
                'mos-glanceclient': 'master',
                'mos-cinderclient': 'master',
                'mos-neutronclient': 'master',
                'mos-novaclient': 'master',
                'mos-horizonclient': 'master']

MOS_PROJECTS.each { project, ref ->
    pipelineJob("${JOB_BASE}/mos/ocata/mos-clients-pip/${project}") {
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
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("att-comdev/${project}")
                description('Gerrit refspec or branch')
            }
            stringParam {
                name ('GERRIT_REFSPEC')
                defaultValue('master')
                description('Gerrit refspec or branch')
            }
            stringParam {
                name ('GERRIT_CHANGE_NUMBER')
                defaultValue('0')
                description('patchset number')
            }
            stringParam {
                name ('GERRIT_EVENT_TYPE')
                defaultValue('patchset-created')
                description('patchset-created or change-merged')
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
                    script(readFileFromWorkspace("${JOB_BASE}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}


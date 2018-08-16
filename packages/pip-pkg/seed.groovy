JOB_BASE='packages'

folder("${JOB_BASE}/pip-pkg")

SEMANTIC_RELEASE_VERSION="0.8"

MOS_PROJECTS = ['mos-keystoneclient',
                'mos-heatclient',
                'mos-glanceclient',
                'mos-cinderclient',
                'mos-neutronclient',
                'mos-novaclient',
                'mos-horizonclient']

MOS_PROJECTS.each { project ->
    pipelineJob("${JOB_BASE}/pip-pkg/${project}") {
        // limit surge of patchsets
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '3'
            }
        }

        parameters {
            stringParam {
                defaultValue('master')
                description('Default branch for manual build.\n\n')
                name ('PROJECT_REF')
            }
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("${project}")
                description('Gerrit project')
            }
            stringParam {
                name ('GERRIT_REFSPEC')
                defaultValue('')
                description('Gerrit refspec')
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
            stringParam {
                name ('ARTF_LOCAL_PYPI_URL')
                defaultValue("https://${ARTF_WEB_URL}/api/pypi/pypi-local")
                description('Artifactory repo url')
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
                    script(readFileFromWorkspace("${JOB_BASE}/pip-pkg/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}

JOB_BASE='packages'

folder("${JOB_BASE}/pip-pkg")

MOS_PROJECTS = ['mos-keystoneclient',
                'mos-heatclient',
                'mos-glanceclient',
                'mos-cinderclient',
                'mos-neutronclient',
                'mos-novaclient',
                'mos-ceilometerclient',
                'mos-swiftclient',
                'mos-openstackclient']

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
                name ('GERRIT_PROJECT')
                defaultValue("${project}")
                description('Gerrit project')
            }
            stringParam {
                name ('GERRIT_REFSPEC')
                defaultValue('refs/heads/master')
                description('Gerrit refspec')
            }
            stringParam {
                name ('GERRIT_PATCHSET_REVISION')
                defaultValue('')
                description('Patchset revision')
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

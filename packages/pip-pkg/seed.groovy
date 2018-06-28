JOB_BASE='packages/pip-pkg'

folder("${JOB_BASE}/mos")

SEMANTIC_RELEASE_VERSION="0.8"

MOS_PROJECTS = ['mos-keystoneclient': 'master',
                'mos-heatclient': 'master',
                'mos-glanceclient': 'master',
                'mos-cinderclient': 'master',
                'mos-neutronclient': 'master',
                'mos-novaclient': 'master',
                'mos-horizonclient': 'master']

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
                trim(true)
            }
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("att-comdev/${project}")
                description('Gerrit refspec or branch')
                trim(true)
            }
            stringParam {
                name ('GERRIT_REFSPEC')
                defaultValue('master')
                description('Gerrit refspec or branch')
                trim(true)
            }
            stringParam {
                name ('GERRIT_CHANGE_NUMBER')
                defaultValue('0')
                description('patchset number')
                trim(true)
            }
            stringParam {
                name ('GERRIT_EVENT_TYPE')
                defaultValue('patchset-created')
                description('patchset-created or change-merged')
                trim(true)
            }
            stringParam {
                name ('ARTF_LOCAL_PYPI_URL')
                defaultValue("https://${ARTF_WEB_URL}/api/pypi/pypi-local")
                description('Artifactory repo url')
                trim(true)
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

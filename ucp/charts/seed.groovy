folder("UCP/charts")
pipelineJob("UCP/charts/helm-toolkit-compatibility") {
    parameters {
        stringParam {
            name ('PROJECT_LIST')
            defaultValue('armada deckhand drydock promenade shipyard')
            description('Projects')
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
        stringParam {
            name ('CICD_REFSPEC')
            defaultValue('master')
            description('refspec that have build_charts script')
        }
    }
    triggers {
        gerritTrigger {
            serverName('OS-CommunityGerrit')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("openstack/openstack-helm")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("**")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('REG_EXP')
                            pattern('helm-toolkit/.*')
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                changeMerged()
                patchsetCreated {
                   excludeDrafts(true)
                   excludeTrivialRebase(true)
                   excludeNoCodeChange(true)
                }
            }
        }
        definition {
            cps {
                script(readFileFromWorkspace('ucp/charts/Jenkinsfile'))
                sandbox()
            }
        }
    }
}

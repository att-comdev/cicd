JOB_FOLDER="charts/openstack"
JOB_NAME="${JOB_FOLDER}/helm-toolkit-compatibility"
folder(JOB_FOLDER)
pipelineJob(JOB_NAME) {
    parameters {
        stringParam {
            name ('PROJECT_LIST')
            defaultValue('armada deckhand drydock promenade shipyard')
            description('UCP Projects')
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('master')
            description('Gerrit refspec or branch')
        }
        stringParam {
            name ('GERRIT_EVENT_TYPE')
            defaultValue('patchset-created')
            description('patchset-created or change-merged')
        }
    }
    triggers {
        gerritTrigger {
            serverName('OS-CommunityGerrit')
            silentMode(true)
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
                script(readFileFromWorkspace("${JOB_NAME}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

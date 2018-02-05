JOB_FOLDER="charts/openstack"
OPENSTACK_REPO='openstack/openstack-helm-infra'
folder(JOB_FOLDER)

def projects = ['calico':'calico/*']

projects.each { project_name, project_repo ->
    pipelineJob("${JOB_FOLDER}/${project_name}") {
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("${OPENSTACK_REPO}")
                description('Gerrit project')
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
                serverName('OS-CommunityGerrit')
                silentMode(true)
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/openstack-helm-infra")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType('REG_EXP')
                                pattern("${project_repo}")
                            }
                            filePath {
                                compareType('REG_EXP')
                                pattern("Makefile")
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
                    script(readFileFromWorkspace("charts/att-comdev/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}

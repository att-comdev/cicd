JOB_FOLDER="charts/att-comdev"
folder(JOB_FOLDER)

def projects = ['jenkins',
                'artifactory-ha',
                'distribution',
                'mission-control',
                'xray']

projects.each { project_name ->
    JOB_BASE_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
        description("This job builds the helm charts for ${JOB_BASE_NAME}")
        logRotator {
            daysToKeep(90)
        }
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("att-comdev/charts")
                description('Gerrit refspec or branch')
                trim (true)
            }
            stringParam {
                name ('GERRIT_REFSPEC')
                defaultValue('master')
                description('Gerrit refspec or branch')
                trim (true)
            }
            stringParam {
                name ('GERRIT_CHANGE_NUMBER')
                defaultValue('0')
                description('patchset number')
                trim (true)
            }
            stringParam {
                name ('GERRIT_EVENT_TYPE')
                defaultValue('patchset-created')
                description('patchset-created or change-merged')
                trim (true)
            }
        }
        triggers {
            gerritTrigger {
                serverName('Gerrithub-jenkins-temp')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/charts")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType("ANT")
                                pattern("${project_name}/**")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                }
                triggerOnEvents {
                    changeMerged()
                    patchsetCreated {
                        excludeDrafts(true)
                        excludeTrivialRebase(false)
                        excludeNoCodeChange(false)
                    }
                    commentAddedContains {
                        commentAddedCommentContains('recheck')
                    }
                }
            }
            definition {
                cps {
                    script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                    sandbox(false)
                }
            }
        }
    }
}

// This pipeline builds the charts in att-comdev/charts
// for any changes in helm-toolkit to ensure charts are not broken
JOB_BASE_NAME="openstack-helm-infra"
pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
    description('This job builds the helm charts of all components in att-comdev/charts \n' +
                'for helm-toolkit changes in openstack/openstack-helm-infra')
    logRotator {
        daysToKeep(90)
    }
    triggers {
        gerritTrigger {
            silentMode(true)
            serverName('OS-CommunityGerrit')
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
                            compareType("ANT")
                            pattern("helm-toolkit/**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                changeMerged()
            }
        }
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                sandbox(false)
            }
        }
    }
}

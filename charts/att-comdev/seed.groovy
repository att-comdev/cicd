JOB_FOLDER="charts/att-comdev"
folder(JOB_FOLDER)

def projects = ['jenkins',
                'artifactory-ha',
                'distribution',
                'mission-control',
                'xray']

projects.each { project_name ->
    JOB_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("att-comdev/charts/${project_name}")
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
            //stringParam('RELEASE_CURRENT_KEY',"${currentReleaseKey}.chart.${project_name}.dev.current")
            //stringParam('RELEASE_STATUS_KEY',"${currentStatusKey}.chart.${project_name}.dev.status")
        }
        triggers {
            gerritTrigger {
                serverName('Gerrithub-jenkins')
                silentMode(true)
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/charts/${project_name}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
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
                    script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}

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
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("att-comdev/charts")
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
                serverName('Gerrithub-jenkins')
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

JOB_FOLDER="images/openstack/openstack-helm-images"
folder(JOB_FOLDER)

def projects = ['ceph-daemon',
                ]

projects.each { project_name ->
    JOB_BASE_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
        description("This job builds the image for ${JOB_BASE_NAME}")
        logRotator {
            daysToKeep(90)
        }
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("openstack/openstack-helm-images")
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
                serverName('OS-CommunityGerrit')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/openstack-helm-images")
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

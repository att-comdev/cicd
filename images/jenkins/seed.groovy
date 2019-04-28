JOB_FOLDER="images/jenkins"
def project_name = "jenkins"
pipelineJob("${JOB_FOLDER}") {
        description("This job builds the image for ${project_name}")
        logRotator {
            daysToKeep(90)
        }
        parameters {
            stringParam {
                name ('GERRIT_REFSPEC')
                defaultValue('master')
                description('Gerrit refspec or branch')
                trim(true)
            }
            stringParam {
                name ('GERRIT_PATCHSET_REVISION')
                defaultValue('0')
                description('patchset revision')
                trim(true)
            }
            stringParam {
                name ('GERRIT_EVENT_TYPE')
                defaultValue('patchset-created')
                description('patchset-created or change-merged')
                trim(true)
            }
            stringParam {
                name ('DEFAULT_TAG')
                defaultValue('lts')
                description('Tag of the upstream image that needs to be built')
                trim(true)
            }
        }
        triggers {
            gerritTrigger {
                silentMode(true)
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
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/dockerfiles")
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

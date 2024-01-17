base_path = 'cicd'
job_path = "${base_path}/SuperSeed"
folder("${base_path}")

pipelineJob("${job_path}") {
    logRotator {
        daysToKeep(90)
    }
    parameters {
        stringParam('SEED_PATH', '', 'Comma delimited Seed path \n' +
                        'Example: cicd/SuperSeed/seed.groovy,cicd/NodeCleanup/seed.groovy')
        stringParam('RELEASE_FILE_PATH', '', 'File with a list of seed.groovy files')
        stringParam('GERRIT_REFSPEC', 'origin/master', "Gerrit Refspec")

        // this parameter is not needed for the actual SuperSeed job
        // all the interactions with git are done using refspec
        // but existing jobs sometimes use GERRIT_REFSPEC variable during seeding
        // so this parameter is kept here for backward compatibility.
        stringParam('GERRIT_BRANCH', 'master',"Branch for provided GERRIT_REFSPEC")

        stringParam('GERRIT_HOST', 'review.gerrithub.io', 'Gerrit Host')
        stringParam('GERRIT_PROJECT', 'att-comdev/cicd', 'Project on Gerrit')
    }

    properties {
        pipelineTriggers {
            triggers {
                gerritTrigger {
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern('att-comdev/cicd')
                            branches {
                                branch {
                                    compareType('ANT')
                                    pattern('**/master')
                                }
                            }
                            disableStrictForbiddenFileVerification(false)
                        }
                        gerritProject {
                            compareType('REG_EXP')
                            pattern("^nc-cicd\$")
                            branches {
                                branch {
                                    compareType('ANT')
                                    pattern('**/main')
                                }
                            }
                            disableStrictForbiddenFileVerification(false)
                        }
                    }
                    triggerOnEvents {
                        /// PatchsetCreated trigger should be manually enabled on staging:
                        patchsetCreated {
                            excludeDrafts(true)
                            excludeTrivialRebase(false)
                            excludeNoCodeChange(false)
                        }

                        commentAddedContains {
                            commentAddedCommentContains('recheck')
                        }

                        /// changeMerged trigger for production:
                        changeMerged()
                    }
                }
                definition {
                    cps {
                        script(readFileFromWorkspace("cicd/SuperSeed/superseed.Jenkinsfile"))
                    }
                }
            }
        }
    }
}

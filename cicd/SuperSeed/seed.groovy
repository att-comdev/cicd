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
        stringParam('GERRIT_REFSPEC', 'main', 'Gerrit Refspec Or Branch Name')
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

                        /// changeMerged trigger for production:
                        changeMerged()
                    }
                }
                definition {
                    cps {
                        script(readFileFromWorkspace("cicd/SuperSeed/superseed.Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}

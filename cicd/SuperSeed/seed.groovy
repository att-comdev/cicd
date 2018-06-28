base_path = "cicd"
job_path = "${base_path}/SuperSeed"
folder("${base_path}")

freeStyleJob("${job_path}") {
    label('master')
    parameters {
        stringParam {
            name ('SEED_PATH')
            defaultValue('')
            description('Seed path. Example: cicd/SuperSeed/seed.groovy')
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('origin/master')
            description('Gerrit refspec')
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('att-comdev/cicd')
            description('Project on Gerrithub')
        }
    }

    triggers {
        gerritTrigger {
            serverName('__ANY__')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("att-comdev/cicd")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**")
                        }
                    }
                    //forbiddenFilePaths {
                    //    filePaths {
                    //        compareType('ANT')
                    //        pattern("vars/**")
                    //    }
                    //    filePaths {
                    //        compareType('ANT')
                    //        pattern("resources/**")
                    //    }
                    //}
                    disableStrictForbiddenFileVerification(false)
                }
                gerritProject {
                    compareType('PLAIN')
                    pattern("nc-cicd")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**")
                        }
                    }
                    //forbiddenFilePaths {
                    //    filePaths {
                    //        compareType('ANT')
                    //        pattern("vars/**")
                    //    }
                    //    filePaths {
                    //        compareType('ANT')
                    //        pattern("resources/**")
                    //    }
                    //}
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
/// PatchsetCreated trigger should be manually enabled on staging:
//                patchsetCreated {
//                   excludeDrafts(true)
//                   excludeTrivialRebase(false)
//                   excludeNoCodeChange(false)
//                }

/// changeMerged trigger for production:
            changeMerged()
            }
        }
    }
    steps {
    //Wipe the workspace:
        wrappers {
            preBuildCleanup()
        }
        shell(readFileFromWorkspace("${job_path}/superseed.sh"))
        dsl {
            external('${BUILD_NUMBER}/seed.groovy')
            //ignoreExisting(true)
            //removeAction('DISABLE')
        }
    }
}

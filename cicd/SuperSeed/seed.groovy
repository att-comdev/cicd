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
            trim(true)
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('origin/master')
            description('Gerrit refspec')
            trim(true)
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('att-comdev/cicd')
            description('Project on Gerrithub')
            trim(true)
        }
    }

    triggers {
        gerritTrigger {
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("att-comdev/cicd")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
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

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
            serverName('Gerrithub-voting')
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
                patchsetCreated {
                   excludeDrafts(true)
                   excludeTrivialRebase(true)
                   excludeNoCodeChange(true)
                }
//FIXME: change the trigger before the merge!!!!:
//            changeMerged()
            }
        }
    }
    steps {
    //Wipe the workspace:
        wrappers {
            preBuildCleanup()
        }
        shell(readFileFromWorkspace("${job_path}/superseed.sh"))
//testing
        File seed = new File("${WORKSPACE}/SEED_PATH.txt")
        if (seed.exists()){
            environmentVariables {
                propertiesFile("SEED_PATH.txt")
                overrideBuildParameters()
            }
            println("${SEED_PATH}")
        }

        dsl {
            external('${SEED_PATH}')
            //ignoreExisting(true)
            //removeAction('DISABLE')
        }
    }
}

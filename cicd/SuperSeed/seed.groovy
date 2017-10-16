base_path = "cicd/SuperSeed"
freeStyleJob("${base_path}") {
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
            description('Gerrit refspec, you can leave SEED_PATH empty if you have refspec')
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('att-comdev/cicd')
            description('Project name on Gerrithub')
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
        shell(readFileFromWorkspace("${base_path}/superseed.sh"))
        dsl {
            external(build.buildVariableResolver.resolve(SEED_PATH))
            //ignoreExisting(true)
            //removeAction('DISABLE')
        }
    }
}

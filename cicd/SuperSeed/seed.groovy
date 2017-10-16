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
            description('Gerrit refspec')
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('att-comdev/cicd')
            description('Project name on Gerrithub')
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
    //WipeOut the workspace:
        wrappers {
            preBuildCleanup()
        }
        shell(readFileFromWorkspace("${base_path}/superseed.sh"))
        dsl {
            external('jobs/seed.groovy')
            ignoreExisting(true)
            removeAction('DELETE')
        }
    }
}

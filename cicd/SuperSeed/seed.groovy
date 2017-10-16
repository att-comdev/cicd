base_path = "cicd/SuperSeed"

freeStyleJob("${base_path}") {
    label('master')
//    parameters {
//        stringParam {
//            defaultValue(GERRIT_REFSPEC)
//            description('Pass att-comdev/cicd code refspec to the job')
//            name ('CICD_GERRIT_REFSPEC')
//        }
//    }

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
//            changeMerged()
            }
        }
    }
    steps {
//WipeOut the workspace:
        deleteDirs()
        shell(readFileFromWorkspace("${base_path}/superseed.sh"))
        dsl {
//            text(readFileFromWorkspace('jobs/seed.groovy'))
            external('jobs/seed.groovy')
            ignoreExisting(true)
            removeAction('DELETE')
        }
    }
}

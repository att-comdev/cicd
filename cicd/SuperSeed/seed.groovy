base_path = "cicd/SuperSeed"

pipelineJob("${base_path}") {

//    parameters {
//        stringParam {
//            defaultValue(GERRIT_REFSPEC)
//            description('Pass att-comdev/cicd code refspec to the job')
//            name ('CICD_GERRIT_REFSPEC')
//        }
//    }

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
//            changeMerged()
            }
        }

        steps {
            shell(readFileFromWorkspace("${base_path}/superseed.sh"))
        }

        definition {
            cps {
                script(readFileFromWorkspace('jobs/seed.groovy'))
                sandbox()
            }
        }
    }
}

base_path = "cicd"
job_path = "${base_path}/SuperSeed"
folder("${base_path}")

freeStyleJob("${job_path}") {
    label('master')
    parameters {
        stringParam {
            name ('SEED_PATH')
            defaultValue('')
            description('Comma delimted Seed path \n' +
                        'Example: cicd/SuperSeed/seed.groovy,cicd/NodeCleanup/seed.groovy')
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('origin/master')
            description('Gerrit refspec')
        }
        stringParam {
            name ('GERRIT_HOST')
            defaultValue('review.gerrithub.io')
            description('Gerrit host')
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('att-comdev/cicd')
            description('Project on Gerrit')
        }
    }

    triggers {
        gerritTrigger {
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
                gerritProject {
                    compareType('REG_EXP')
                    pattern("^nc-cicd\$")
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
            credentialsBinding {
                usernamePassword('JENKINS_USER', 'JENKINS_TOKEN', 'jenkins-token')
            }
        }
        shell(readFileFromWorkspace("${job_path}/superseed.sh"))
        dsl {
            external('${BUILD_NUMBER}/**/seed*.groovy')
            //ignoreExisting(true)
            //removeAction('DISABLE')
        }
    }
}

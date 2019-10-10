base_path = "cicd"
job_path = "${base_path}/SuperSeed"
folder("${base_path}")

freeStyleJob("${job_path}") {
    logRotator{
        daysToKeep(90)
    }
    label('master')
    parameters {
        stringParam('SEED_PATH', '', 'Comma delimited Seed path \n' +
                        'Example: cicd/SuperSeed/seed.groovy,cicd/NodeCleanup/seed.groovy')
        stringParam('RELEASE_FILE_PATH', '', 'File that points to a list of seed.groovy to execute for a Cloudharbor site')
        stringParam('GERRIT_REFSPEC', 'origin/master', "Gerrit Refspec")
        stringParam('GERRIT_BRANCH', 'master',"Branch for provided GERRIT_REFSPEC")
        stringParam('GERRIT_HOST', 'review.gerrithub.io', 'Gerrit Host')
        stringParam('GERRIT_PROJECT', 'att-comdev/cicd', 'Project on Gerrit')
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
                patchsetCreated {
                   excludeDrafts(true)
                   excludeTrivialRebase(false)
                   excludeNoCodeChange(false)
                }

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
            sshAgent("${INTERNAL_GERRIT_KEY}")
        }
        shell(readFileFromWorkspace("${job_path}/superseed.sh"))
        jobDsl {
            targets('${BUILD_NUMBER}/**/seed*.groovy')
            // Add ignoreMissingFiles to ignore when seeds are not copied for patchsets
            ignoreMissingFiles(true)
            //ignoreExisting(true)
            //removeAction('DISABLE')
        }
    }
}

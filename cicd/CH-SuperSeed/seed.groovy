release_branch = "v1.4"
base_path = "cicd"
job_path = "${base_path}/CH-SuperSeed"
folder(base_path)

// needs to be a freestyle job to be able to process DSLs
freeStyleJob(job_path) {
    logRotator{
        daysToKeep(90)
    }
    label('master')
    parameters {
        stringParam{
            name ('RELEASE_FILE_PATH')
            defaultValue('src/release_file')
            description("File that points to a list of seed.groovy's to execute for a Cloudharbor site")
            //trim(true)
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('origin/master')
            description('Gerrit refspec')
            //trim(true)
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('nc-cicd')
            description('Project on Gerrit')
            //trim(true)
        }
    }

    triggers {
        gerritTrigger {
            gerritProjects {
                gerritProject {
                    compareType('REG_EXP')
                    pattern("^cicd\$")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern(release_branch)
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
                gerritProject {
                    compareType('REG_EXP')
                    pattern("^nc-cicd\$")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern(release_branch)
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
/// PatchsetCreated trigger should be manually enabled on staging:
                //patchsetCreated {
                //   excludeDrafts(true)
                //   excludeTrivialRebase(false)
                //   excludeNoCodeChange(false)
                //}

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
        jobDsl {
            targets('${BUILD_NUMBER}/**/seed*.groovy')
            // Add ignoreMissingFiles to ignore when seeds are not copied for patchsets
            ignoreMissingFiles(true)
            //ignoreExisting(true)
            //removeAction('DISABLE')
        }
    }
}

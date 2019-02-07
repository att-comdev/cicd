base_path = "cicd"
job_path = "${base_path}/SuperSeed"
folder("${base_path}")

freeStyleJob("${job_path}") {
    label('master')
    parameters {
        stringParam {
            name ('SEED_PATH')
            defaultValue('')
            description('Comma delimited Seed path \n' +
                        'Example: cicd/SuperSeed/seed.groovy,cicd/NodeCleanup/seed.groovy')
        }
        stringParam{
            name ('RELEASE_FILE_PATH')
            defaultValue('')
            description("File that points to a list of seed.groovy's to execute for a Cloudharbor site")
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
                            pattern("**")
                        }
                    }
                    forbiddenFilePaths {
                        filePath {
                            compareType('ANT')
                            pattern("**/charts/**")
                        }
                    }
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
                    forbiddenFilePaths {
                        filePath {
                            compareType('ANT')
                            pattern("**/charts/**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
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
            //ignoreMissingFiles(true)
            //ignoreExisting(true)
            //removeAction('DISABLE')
        }
    }
}

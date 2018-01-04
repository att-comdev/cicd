base_path = "cicd"
job_path = "${base_path}/SuperSeed_Review"
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
                    forbiddenFilePaths {
                        filePath {
                            compareType('ANT')
                            pattern("vars/**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                patchsetCreated {
                   excludeDrafts(true)
                   excludeTrivialRebase(false)
                   excludeNoCodeChange(false)
                }
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

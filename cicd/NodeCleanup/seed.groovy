base_path = "cicd"
job_path = "${base_path}/NodeCleanup"
folder("${base_path}")

freeStyleJob("${job_path}") {
    label('master')
    parameters {
        stringParam {
            name ('DELETE_NODENAME')
            defaultValue('')
            description('Node to be deleted')
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
    definition {
        cps {
            script(readFileFromWorkspace("${job_path}/Jenkinsfile"))
            sandbox()
        }
    }
}

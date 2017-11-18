
def PROJECTS = [['name': 'dekchand'],
                ['name': 'promenade']] 

def PROJECT_FOLDER="images/att-comdev/ucp"
folder("${PROJECT_FOLDER}")

for (project in PROJECTS) {
    pipelineJob("${PROJECT_FOLDER}/${project.name}") {

        parameters {
            stringParam {
                defaultValue(GERRIT_REFSPEC)
                description('Pass att-comdev/cicd refspec')
                name ('CICD_GERRIT_REFSPEC')
            }
        }

        triggers {
            gerritTrigger {
                silentMode(false)
                serverName('Gerrithub-jenkins')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/${project.name}")
                        branches {
                            branch {
                                compareType('ANT')
                                pattern('**')
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
                    changeMerged()
                }
            }
            definition {
                cps {
                    script(readFileFromWorkspace("${PROJECT_FOLDER}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}


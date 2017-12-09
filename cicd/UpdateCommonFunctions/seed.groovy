base_path = "cicd"
job_path = "${base_path}/UpdateCommonFunctions"
project = "att-comdev/cicd"
node_label='local-vm-launch'

//Common functions dir: ${HOME}/CommonFunctions
//See ${job_path}/script.sh for more details!!!

folder(base_path)
freeStyleJob(job_path){
    label(node_label)
    triggers {
        gerritTrigger {
            silentMode(true)
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern(project)
                    branches {
                        branch {
                            compareType('ANT')
                            pattern('**/master')
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('REG_EXP')
                            pattern('common/.*')
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                changeMerged()
            }
        }
    }
    steps {
        //Updating functions:
        shell(readFileFromWorkspace("${job_path}/script.sh"))
    }
}

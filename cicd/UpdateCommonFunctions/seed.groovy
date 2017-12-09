base_path = "cicd"
job_path = "${base_path}/UpdateCommonFunctions"
project = "att-comdev/cicd"
common_functions_dir="${env.HOME}/CommonFunctions/common"

folder(base_path)
freeStyleJob(job_path){
    label('master')
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
                }
            }
            triggerOnEvents {
                changeMerged()
            }
        }
    }
    steps {
        def folder = new File("${common_functions_dir}/common")
        if( !folder.exists() ) {
            shell("git clone https://review.gerrithub.io/${project} ${common_functions_dir}")
        }else{
            shell("cd ${common_functions_dir} && git pull origin master")
        }
//        shell(readFileFromWorkspace("${job_path}/script.sh"))
    }
}

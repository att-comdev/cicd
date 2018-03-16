JOB_FOLDER="charts/att-comdev"
folder(JOB_FOLDER)

def projects = ['armada':'charts/.*',
                'deckhand':'charts/.*',
                'drydock':'charts/.*',
                'promenade':'charts/.*',
                'shipyard':'charts/.*',
                'maas':'charts/.*',
                'divingbell':'.*']

projects.each { project_name, file_path ->
    JOB_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("att-comdev/${project_name}")
                description('Gerrit refspec or branch')
            }
            stringParam {
                name ('GERRIT_REFSPEC')
                defaultValue('master')
                description('Gerrit refspec or branch')
            }
            stringParam {
                name ('GERRIT_CHANGE_NUMBER')
                defaultValue('0')
                description('patchset number')
            }
            stringParam {
                name ('GERRIT_EVENT_TYPE')
                defaultValue('patchset-created')
                description('patchset-created or change-merged')
            }
        }
        triggers {
            gerritTrigger {
                serverName('Gerrithub-jenkins')
            //    silentMode(true)
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/${project_name}")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType('REG_EXP')
                                pattern("${file_path}")
                            }
                            filePath {
                                compareType('REG_EXP')
                                pattern("Makefile")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                }
                triggerOnEvents {
                    changeMerged()
                    patchsetCreated {
                       excludeDrafts(true)
                       excludeTrivialRebase(true)
                       excludeNoCodeChange(true)
                    }
                }
            }
            definition {
                cps {
                    script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}

JOB_FOLDER="charts/att-comdev"
folder(JOB_FOLDER)

def currentReleaseKey="5EC"
def currentStatusKey="5EC"

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
            stringParam('RELEASE_CURRENT_KEY',"${currentReleaseKey}.chart.${project_name}.dev.current")
            stringParam('RELEASE_STATUS_KEY',"${currentStatusKey}.chart.${project_name}.dev.status")
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


projects = ['apiserver':'charts/apiserver/.*',
                'calico':'charts/calico/.*',
                'controller_manager':'charts/controller_manager/.*',
                'coredns':'charts/coredns/.*',
                'etcd':'charts/etcd/.*',
                'haproxy':'charts/haproxy/.*',
                'scheduler':'charts/scheduler/.*',
                'proxy':'proxy/.*']

projects.each { project_name, file_path ->
    JOB_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("att-comdev/promenade")
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
            stringParam('RELEASE_CURRENT_KEY',"${currentReleaseKey}.chart.${project_name}.dev.current")
            stringParam('RELEASE_STATUS_KEY',"${currentStatusKey}.chart.${project_name}.dev.status")
        }
        triggers {
            gerritTrigger {
                serverName('Gerrithub-jenkins')
            //    silentMode(true)
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/promenade")
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


projects = ['tiller':'charts/tiller/.*']

projects.each { project_name, file_path ->
    JOB_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("att-comdev/armada")
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
            stringParam('RELEASE_CURRENT_KEY',"${currentReleaseKey}.chart.${project_name}.dev.current")
            stringParam('RELEASE_STATUS_KEY',"${currentStatusKey}.chart.${project_name}.dev.status")
        }
        triggers {
            gerritTrigger {
                serverName('Gerrithub-jenkins')
            //    silentMode(true)
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("att-comdev/armada")
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
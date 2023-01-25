JOB_FOLDER="images/openstack/helm-images"
folder(JOB_FOLDER)

def projects = ['ceph-daemon',
                'ceph-utility',
                'ceph-config-helper',
                'ceph-rbd-provisioner',
                'ceph-cephfs-provisioner',
                'calicoctl-utility',
                'elasticsearch-s3',
                'fluentd',
                'nagios',
                'prometheus-openstack-exporter']

projects.each { project_name ->
    JOB_BASE_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
        description("This job builds the image for ${JOB_BASE_NAME}")
        logRotator {
            daysToKeep(90)
        }
        parameters {
            stringParam('GERRIT_PROJECT', "openstack/openstack-helm-images","Gerrit refspec or branch")
            stringParam('GERRIT_REFSPEC','master',"Gerrit refspec or branch")
            stringParam('GERRIT_PATCHSET_REVISION','0','patchset revision')
            stringParam('GERRIT_EVENT_TYPE','patchset-created','patchset-created or change-merged')
            stringParam('GERRIT_CHANGE_URL','manual','Change URL')
            if (project_name == 'calicoctl-utility') {
                stringParam('CALICOCTL_VERSION','v3.4.0','Calicoctl base image version')
            }
        }
        triggers {
            gerritTrigger {
                silentMode(true)
                serverName('ATT-airship-CI')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/openstack-helm-images")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType("ANT")
                                pattern("**/${JOB_BASE_NAME}/**")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                }
                triggerOnEvents {
                    changeMerged()
                    patchsetCreated {
                       excludeDrafts(false)
                       excludeTrivialRebase(false)
                       excludeNoCodeChange(false)
                    }
                }
            }
            definition {
                cps {
                    script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                    sandbox(false)
                }
            }
        }
    }
}

// Build Ceph Focal packages explicitly.
def ceph_focal_projects = ['ceph-daemon',
                'ceph-utility',
                'ceph-config-helper',
                'ceph-rbd-provisioner',
                'ceph-cephfs-provisioner']

ceph_focal_projects.each { project_name ->
    JOB_BASE_NAME=project_name + "_focal"
    pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
        description("This job builds the Ubuntu Focal based image for ${JOB_BASE_NAME}")
        logRotator {
            daysToKeep(90)
        }
        parameters {
            stringParam('GERRIT_PROJECT', "openstack/openstack-helm-images","Gerrit refspec or branch")
            stringParam('GERRIT_REFSPEC','master',"Gerrit refspec or branch")
            stringParam('GERRIT_PATCHSET_REVISION','0','patchset revision')
            stringParam('GERRIT_EVENT_TYPE','patchset-created','patchset-created or change-merged')
            stringParam('GERRIT_CHANGE_URL','manual','Change URL')
            stringParam('UBUNTU_VERSION','ubuntu_focal','Ubuntu version to build images for')
            stringParam('DIR_NAME', project_name, 'A folder in the repo where Dockerfile is')
        }
        triggers {
            gerritTrigger {
                silentMode(true)
                serverName('ATT-airship-CI')
                gerritProjects {
                    gerritProject {
                        compareType('PLAIN')
                        pattern("openstack/openstack-helm-images")
                        branches {
                            branch {
                                compareType("ANT")
                                pattern("**")
                            }
                        }
                        filePaths {
                            filePath {
                                compareType("ANT")
                                pattern("**/${project_name}/**")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                }
                triggerOnEvents {
                    changeMerged()
                    patchsetCreated {
                       excludeDrafts(false)
                       excludeTrivialRebase(false)
                       excludeNoCodeChange(false)
                    }
                }
            }
            definition {
                cps {
                    script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                    sandbox(false)
                }
            }
        }
    }
}

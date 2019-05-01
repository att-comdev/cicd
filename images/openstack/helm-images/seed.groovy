JOB_FOLDER="images/openstack/helm-images"
folder(JOB_FOLDER)

def projects = ['ceph-daemon',
                'ceph-utility',
                'ceph-config-helper',
                'ceph-rbd-provisioner',
                'ceph-cephfs-provisioner',
                'mysql-client-utility',
                'openstack-utility',
                'calicoctl-utility',
                'openvswitch']

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
            stringParam('CALICOCTL_VERSION','v3.4.0','Calicoctl base image version')
            stringParam('OVS_VERSION','2.10.1','openvswitch version')
            stringParam('DPDK_VERSION','17.11.5','DPDK version')
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
                        disableStrictForbiddenFileVerification(false)
                    }
                }
                triggerOnEvents {
                    changeMerged()
                    commentAddedContains {
                        commentAddedCommentContains('recheck')
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

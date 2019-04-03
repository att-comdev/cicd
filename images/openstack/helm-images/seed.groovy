JOB_FOLDER="images/openstack/helm-images"
folder(JOB_FOLDER)

def projects = ['ceph-daemon',
                'ceph-utility',
                'ceph-config-helper',
                'calicoctl-utility']

projects.each { project_name ->
    JOB_BASE_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
        description("This job builds the image for ${JOB_BASE_NAME}")
        logRotator {
            daysToKeep(90)
        }
        parameters {
            stringParam {
                name ('GERRIT_PROJECT')
                defaultValue("openstack/openstack-helm-images")
                description('Gerrit refspec or branch')
                trim(true)
            }
            stringParam {
                name ('GERRIT_REFSPEC')
                defaultValue('master')
                description('Gerrit refspec or branch')
                trim(true)
            }
            stringParam {
                name ('GERRIT_PATCHSET_REVISION')
                defaultValue('0')
                description('patchset revision')
                trim(true)
            }
            stringParam {
                name ('GERRIT_EVENT_TYPE')
                defaultValue('patchset-created')
                description('patchset-created or change-merged')
                trim(true)
            }
            stringParam {
                name ('GERRIT_CHANGE_URL')
                defaultValue('manual')
                description('Change URL')
                trim(true)
            }
            stringParam {
                name ('CALICOCTL_VERSION')
                defaultValue('v3.4.0')
                description('Calicoctl base image version')
                trim(true)
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

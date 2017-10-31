folder("UCP/charts")
NODE_NAME="ucp-charts-${BUILD_NUMBER}"
NODE_TMPL = "charts/ubuntu.m1.large.yaml"
//ARTF_URL = env.ARTF_WEB_URL
CURRENT_VERSION = "0.1.0.${GERRIT_CHANGE_NUMBER}"
PS_ARTF_REPO = "charts/ucp/ps/"+CURRENT_VERSION
ARTF_REPO = "charts/ucp/"+CURRENT_VERSION
def funcs
pipelineJob("UCP/charts/all") {
    parameters {
        stringParam {
            name ('PROJECTS_LIST')
            defaultValue('armada shipyard')
            description('Project name')
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('master')
            description('Gerrit refspec or branch')
        }
    }
    triggers {
        gerritTrigger {
            serverName('OS-CommunityGerrit')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("openstack/openstack-helm")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("**")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('REG_EXP')
                            pattern('helm-toolkit/.*')
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
//        definition {
//            cps {
//                script(readFileFromWorkspace('ucp/charts/Jenkinsfile'))
//                sandbox()
//            }
//        }
    }
//}
//=======================


    try{
        stage('Launch Chart Node'){
            node('master') {
                scm {
                    git {
                        remote {
                            name('master')
                            url('https://review.gerrithub.io/att-comdev/cicd')
                        }
                    }
                }
                funcs = load "${WORKSPACE}/common/funcs.groovy"
                funcs.jenkins_slave_launch(NODE_NAME, "${HOME}/${NODE_TMPL}")
            }
        }

        stage('Nodes Wait'){
            timeout (10) {
                node (NODE_NAME) {
                    echo "Verifying that Jenkins node comes up."
                }
            }
        }
    }

    finally {
        stage('Delete Charts Node'){
           node('master') {
               funcs.jenkins_slave_destroy(NODE_NAME)
            }
        }
    }
}

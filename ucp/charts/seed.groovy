folder("UCP/charts")
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

JENKINS_VM_LAUNCH = 'local-vm-launch'
NODE_NAME="ucp-charts-${BUILD_NUMBER}"
NODE_TMPL = "charts/ubuntu.m1.large.yaml"
//ARTF_URL = env.ARTF_WEB_URL
CURRENT_VERSION = "0.1.0.${GERRIT_CHANGE_NUMBER}"
PS_ARTF_REPO = "charts/ucp/ps/"+CURRENT_VERSION
ARTF_REPO = "charts/ucp/"+CURRENT_VERSION
def funcs

try{
    stage('Spawn Chart Node'){
        node(JENKINS_VM_LAUNCH) {
            checkout poll: false,
            scm: [$class: 'GitSCM',
                  branches: [[name: '*/master']],
                  doGenerateSubmoduleConfigurations: false,
                  extensions: [],
                  submoduleCfg: [],
                  userRemoteConfigs: [[url: 'https://review.gerrithub.io/att-comdev/cicd']]]

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
    node(NODE_NAME) {
//        stage('Checkout') {
//                checkout poll: false,
//                scm: [$class: 'GitSCM',
//                    branches: [[name: '$GERRIT_REFSPEC']],
//                    doGenerateSubmoduleConfigurations: false,
//                    extensions: [[$class: 'CleanBeforeCheckout']],
//                    submoduleCfg: [],
//                    userRemoteConfigs: [[refspec: 'refs/changes/*:refs/changes/*',
////                    url: 'https://git.openstack.org/openstack/openstack-helm']]]
////                    url: "https://review.gerrithub.io/${env.GERRIT_PROJECT}"]]]
//                    url: "https://review.gerrithub.io/att-comdev/cicd"]]]
//        }
        stage('Build Charts') {
            shell(readFileFromWorkspace(ucp/charts/build_charts.sh))
        }
    }
} finally {
    stage('Delete Jenkins Node'){
       node(JENKINS_VM_LAUNCH) {
           funcs.jenkins_slave_destroy(NODE_NAME)
        }
    }
}
}

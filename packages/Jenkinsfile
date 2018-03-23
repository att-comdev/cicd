JENKINS_VM_LAUNCH='local-vm-launch'
NODE_NAME="${JOB_BASE_NAME}-${BUILD_NUMBER}"
NODE_TMPL="docker/ubuntu.m1.medium.yaml"

/* start of bash script. */
def build_charts = '''
echo "=== Test build charts ==="
env
pwd
id
'''
/* end of bash script. */

vm(NODE_NAME, NODE_TMPL) {
    stage('Test stage here'){
        sh 'echo "Hello"'
        def status = sh(returnStatus: true, script: build_charts)
        if (status != 0) {
            currentBuild.result = 'FAILED'
        }
    }
}

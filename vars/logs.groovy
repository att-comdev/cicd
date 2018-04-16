//Methods that can be used to retrieve logs from Jenkins jobs


/**
 * getJenkinsConsoleOutput - This method will curl the Jenkins console output and place it into 
 *    log file that will be published to Artifactory.  We do not want this to error a pipeline but
 *    we do want to be notified when it is not publishing anything.
 *
 **/
def getJenkinsConsoleOutput() {
    sh 'mkdir -p jenkinsConsole'
    if("x${GERRIT_CHANGE_NUMBER}" != "x" && "x${GERRIT_PATCHSET_NUMBER}" != "x" && "x${BUILD_URL}" != "x") {
        cmd = "curl -s -o ./jenkinsConsole/${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}.log ${BUILD_URL}consoleText"
    } else if("x${BUILD_URL}" != "x" && "x${JOB_BASE_NAME}" != "x" && "x${BUILD_NUMBER}" != "x") {
        cmd  = "curl -s -o ./jenkinsConsole/${JOB_BASE_NAME}-${BUILD_NUMBER}.log ${BUILD_URL}consoleText"
    } else {
        notify.msg("No logs published, verify this is a valid Jenkins job.")
    }
    
    if("x${cmd}" != "x"){
        sh (script: cmd, returnStatus: true)
        publish.uploadArtifacts("./jenkinsConsole/*.log", "logs/${JOB_NAME}/")
    } else {
        notify.msg("No logs published, please investigate.")
    }
}
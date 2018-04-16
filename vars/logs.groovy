//Methods that can be used to retrieve logs from Jenkins jobs


/**
 * getJenkinsConsoleOutput - This method will curl the Jenkins console output and place it into
 *    log file that will be published to Artifactory.  We do not want this to error a pipeline but
 *    we do want to be notified when it is not publishing anything.
 *
 **/
def getJenkinsConsoleOutput() {
    logFile = ""
    if("x${GERRIT_CHANGE_NUMBER}" != "x" && "x${GERRIT_PATCHSET_NUMBER}" != "x") {
        logFile = "${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}"
    } else if("x${BUILD_URL}" != "x" && "x${JOB_BASE_NAME}" != "x") {
        logFile = "${JOB_BASE_NAME}-${BUILD_NUMBER}"
    }
    return logFile
}
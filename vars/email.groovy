/*
 * Send email
 *
 * @param email Map of email details
*/
def sendMail(Map email=[:]) {
    // ENABLE_EMAIL as a string allows for configuring as both global env var or pipeline parameter
    if(env.ENABLE_EMAIL == "true") {
        jenkins_instance = JENKINS_URL.split("://")[1].split('/')[0]
        subject = email.find{ it.key == "subject" }?.value ?: "$jenkins_instance ### ${BUILD_TAG} ### ${currentBuild.currentResult}"
        body = email.find{ it.key == "body" }?.value ?: """The job status is [${currentBuild.currentResult}]
                                                   For more details see the build logs at ${BUILD_URL}


                                                    """
        to = email.find{ it.key == "to" }?.value ?: ""
        from = email.find{ it.key == "from" }?.value ?: ""
        attachLog = email.find{ it.key == "attachLog" }?.value ?: false
        attachmentsPattern = email.find{ it.key == "attachmentsPattern" }?.value ?: ""
        recipientProviders = email.find{ it.key == "recipientProviders" }?.value ?: []

        // Usage example: sendMail(attachLog: true, attachmentsPattern: "*.html", to: "mymailid")
        //                sendMail(attachLog: true, attachmentsPattern: "myfile.txt", to: "mymailid,yourmailid")
        //                sendMail(subject:"Build failed", body: "Build failed, check logs", recipientProviders: [culprit()])
        //                sendMail(subject:"Build success", body: "Build succeeded", recipientProviders: [developers(), requestor()])
        //                sendMail(recipientProviders: [requestor()], to: "mymailid")
        // see https://jenkins.io/doc/pipeline/steps/email-ext/#emailext-extended-email for more details
        emailext body: body,
                 subject: subject,
                 to: to,
                 from: from,
                 attachLog: attachLog,
                 attachmentsPattern: attachmentsPattern,
                 recipientProviders: recipientProviders
    }
}

/*
 * Send email
 *
 * @param email Map of email details
*/
def sendMail(Map email=[:]) {
    // ENABLE_EMAIL as a string allows for configuring as both global env var or pipeline parameter
    if(env.ENABLE_EMAIL == "true") {
        // get instance sitename
        jenkins_instance = JENKINS_URL.split("://")[1].split(/[.]/)[1]

        // subject format - [NC Enablement] {job name} {job number (optional)} {status} :: {ACTION Required| Informational}
        default_subject = "[NC Enablement] {$jenkins_instance} {$JOB_NAME} {$BUILD_NUMBER} ${currentBuild.currentResult} :: {ACTION Required}"
        subject = email.find{ it.key == "subject" }?.value ?: default_subject
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
        //                sendMail(subject:"[NC Enablement] jenkins_instance_name {$JOB_NAME} {$BUILD_NUMBER} ${currentBuild.currentResult} :: {ACTION Required}",
        //                         body: "Build failed, check logs", recipientProviders: [culprit()])
        //                sendMail(subject:"[NC Enablement] jenkins_instance_name {$JOB_NAME} {$BUILD_NUMBER} ${currentBuild.currentResult} :: {Informational}",
        //                         body: "Build succeeded", recipientProviders: [developers(), requestor()])
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

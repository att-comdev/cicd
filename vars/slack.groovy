
def msg(String msg, String channel=SLACK_DEFAULT_CHANNEL){
// Usage example:  funcs.slack_msg( "${env.GERRIT_CHANGE_URL} is OK!")
// Custom channel: funcs.slack_msg( "${env.GERRIT_CHANGE_URL} is OK!",'#my_channel')
    slackSend(
        baseUrl: SLACK_URL,
        tokenCredentialId: 'jenkins-slack',
        channel: channel,
        message: "Job <${env.JOB_URL}|${env.JOB_NAME}> said:\n" + msg
    )
}


#!groovy

// test functions here:

def Notify(String channel, String msg){
    slackSend(
        baseUrl:'https://att-comdev.slack.com/services/hooks/jenkins-ci/',
        tokenCredentialId: 'jenkins-slack',
        channel: channel,
        message: "Job <${env.JOB_URL}|${env.JOB_NAME}> said:\n" + msg
    )
}

return this;


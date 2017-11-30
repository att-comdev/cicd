#!groovy

// test functions here:

def Notify(String channel, String msg){
    slackSend(
        baseUrl:'https://att-comdev.slack.com/services/hooks/jenkins-ci/',
        tokenCredentialId: 'jenkins-slack',
        channel: channel,
        message: msg
    )
}

return this;


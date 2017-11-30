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

def gh_clone(String project, String refspec){
    checkout poll: false,
    scm: [$class: 'GitSCM',
         branches: [[name: refspec]],
         doGenerateSubmoduleConfigurations: false,
         extensions: [[$class: 'CleanBeforeCheckout']],
         submoduleCfg: [],
         userRemoteConfigs: [[refspec: 'refs/changes/*:refs/changes/*',
         url: "https://review.gerrithub.io/"+ project ]]]

}

return this;


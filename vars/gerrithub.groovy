
// Required credentials names
//  - jenkins-slack

// Jenkins global env variables (: examples)
// - SLACK_URL : https://att-comdev.slack.com/services/hooks/jenkins-ci/
// - SLACK_DEFAULT_CHANNEL : #test-jenkins


def clone(String project, String refspec){
// Usage example: gerrithub.clone("att-comdev/cicd", "origin/master")
// clone refspec: gerrithub.clone("att-comdev/cicd", "${env.GERRIT_REFSPEC}")
    checkout poll: false,
    scm: [$class: 'GitSCM',
         branches: [[name: refspec]],
         doGenerateSubmoduleConfigurations: false,
         extensions: [[$class: 'CleanBeforeCheckout']],
         submoduleCfg: [],
         userRemoteConfigs: [[refspec: 'refs/changes/*:refs/changes/*',
         url: "https://review.gerrithub.io/" + project ]]]
}


#!groovy

// test functions here:


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


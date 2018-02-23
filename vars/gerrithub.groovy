
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

//This method is used so that we can checkout the patchset to a local
//branch and then rebase it locally with the current master before we build and test
def cloneToBranch(String project, String refspec, String targetDirectory){
    checkout poll: false,
    scm: [$class: 'GitSCM',
              branches: [[name: refspec]],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'LocalBranch',
                            localBranch: 'jenkins'],
                           [$class: 'RelativeTargetDirectory',
                            relativeTargetDir: targetDirectory]],
                            submoduleCfg: [],
                            userRemoteConfigs: [[refspec: 'refs/changes/*:refs/changes/*',
                                                 url: "https://review.gerrithub.io/" + project]]]
    rebase()
}

def rebase(){
    sh '''git config user.email "attcomdev.jenkins@gmail.com"
          git config user.name "Jenkins"
          git pull --rebase origin master'''
}

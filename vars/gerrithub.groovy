def clone(String project, String refspec){
// Usage example: gerrithub.clone("att-comdev/cicd", "origin/master")
// clone refspec: gerrithub.clone("att-comdev/cicd", "${env.GERRIT_REFSPEC}")
    checkout poll: false,
    scm: [$class: 'GitSCM',
         branches: [[name: refspec]],
         doGenerateSubmoduleConfigurations: false,
         extensions: [[$class: 'CleanBeforeCheckout']],
         submoduleCfg: [],
         userRemoteConfigs: [[refspec: '${GERRIT_REFSPEC}',
         url: "https://review.gerrithub.io/" + project ]]]
}

def cloneToBranch(String project, String refspec, String targetDirectory){
//This method is used so that we can checkout the patchset to a local
//branch and then rebase it locally with the current master before we build and test
    checkout poll: false,
    scm: [$class: 'GitSCM',
              branches: [[name: refspec]],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'LocalBranch',
                            localBranch: 'jenkins'],
                           [$class: 'RelativeTargetDirectory',
                            relativeTargetDir: targetDirectory]],
                            submoduleCfg: [],
                            userRemoteConfigs: [[refspec: '${GERRIT_REFSPEC}',
                                                 url: "https://review.gerrithub.io/" + project]]]
}

def rebase(){
//This method will rebase the local checkout with master and then continue build, tests, etc
    sh '''git config user.email "attcomdev.jenkins@gmail.com"
          git config user.name "Jenkins"
          git pull --rebase origin master'''
}

//Replace clone and rebase methods
def checkout(String revision, String branchToClone, String refspec, String targetDirectory){
   if(revision){
       IMAGE_TAG=revision
   }
   cloneToBranch(branchToClone, refspec, targetDirectory)
   if(!revision) {
       dir(env.WORKSPACE+"/"+targetDirectory){
           rebase()
       }
   }
}

def cloneProject(String project, String branch, String refspec, String targetDirectory){
//This method is used so that we can checkout different project
//from any patchset in different pipelines
// Usage example: gerrithub.cloneProject("att-comdev/cicd", "*/master", "refs/XX/XX/XX" "cicd")
    checkout poll: false,
    scm: [$class: 'GitSCM',
              branches: [[name: "${branch}"]],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'LocalBranch',
                            localBranch: 'jenkins'],
                           [$class: 'RelativeTargetDirectory',
                            relativeTargetDir: targetDirectory]],
                            submoduleCfg: [],
                            userRemoteConfigs: [[refspec: "${refspec}",
                                                 url: "https://review.gerrithub.io/" + project]]]
}

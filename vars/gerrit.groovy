def clone(String url, String refspec){
// Usage example: gerrit.clone("gerrit url", "origin/master")
// clone refspec: gerrit.clone("gerrit url", "${env.GERRIT_REFSPEC}")
    checkout poll: false,
    scm: [$class: 'GitSCM',
         branches: [[name: refspec]],
         doGenerateSubmoduleConfigurations: false,
         extensions: [[$class: 'CleanBeforeCheckout']],
         submoduleCfg: [],
         userRemoteConfigs: [[refspec: '${GERRIT_REFSPEC}',
         url: url ]]]
}

/**
 * Given Jenkins credentials, clones Git repository via SSH
 *
 * @param url "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param refspec "xxxx/master" or other refspec
 * @param creds jenkins SSH credentials ID
*/
def clone(String url, String refspec, String creds){
// Usage example: gerrit.clone("ssh://${GERRIT_HOST}/${GERRIT_PROJECT}", '*/master', "jenkins-gerrit-ssh-creds")
    checkout poll: false,
    scm: [$class: 'GitSCM',
         branches: [[name: refspec]],
         doGenerateSubmoduleConfigurations: false,
         extensions: [[$class: 'CleanBeforeCheckout']],
         submoduleCfg: [],
         userRemoteConfigs: [[refspec: '${GERRIT_REFSPEC}',
         url: url,
         credentialsId: creds ]]]
}

def cloneToBranch(String url, String refspec, String targetDirectory){
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
                                                 url: url ]]]
}

/**
 * Given Jenkins credentials, clones Git repository via SSH to the
 * target directory to local branch and then rebase it locally with
 * the current master before we build and test
 *
 * @param url "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param refspec "xxxx/master" or other refspec
 * @param targetDirectory local directory where to clone repo
 * @param creds jenkins SSH credentials ID
*/
def cloneToBranch(String url, String refspec, String targetDirectory, String creds){
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
                                                 url: url,
                                                 credentialsId: creds ]]]
}

def rebase(){
//This method will rebase the local checkout with master and then continue build, tests, etc
    sh '''git config user.email "airship.jenkins@gmail.com"
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

def cloneProject(String url, String branch, String refspec, String targetDirectory){
//This method is used so that we can checkout different project
//from any patchset in different pipelines
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
                                                 url: url ]]]
}

/**
 * Given Jenkins credentials, clones Git repository via SSH to the
 * target directory and allows to checkout different project from any
 * patchset in different pipelines
 *
 * @param url "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param branch branch
 * @param refspec "xxxx/master" or other refspec
 * @param targetDirectory local directory where to clone repo
 * @param creds jenkins SSH credentials ID
*/
def cloneProject(String url, String branch, String refspec, String targetDirectory, String creds){
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
                                                 url: url,
                                                 credentialsId: creds ]]]
}

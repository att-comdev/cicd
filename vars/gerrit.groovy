import groovy.json.JsonSlurperClassic

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

/**
 * Retrieves the commit identifier of an "open" Gerrit patchset,
 * with a given topic set. Especially useful to get cross-repo
 * dependencies
 *
 * @param repo The repository to search for an "open" patchset with a given topic
 * @param url The url of the Gerrit to check against; ssh user included - e.g. "abc123@gerrit.foo.bar"
 * @param port The port Gerrit is running on
 * @return commit The commitId of the "open" patchset with a given topic. If said PS doesn't exist, "master".
 */
def getTopicCommitId(repo, url, port) {
    // If triggering repo includes a topic
    if("${GERRIT_TOPIC}" != null && "${GERRIT_TOPIC}" != "") {
        def topicJson = sh(script: "ssh -p ${port} ${url} gerrit query --format=JSON topic:${GERRIT_TOPIC} status:open project:${repo}", returnStdout: true).trim()
        def topicData = new JsonSlurperClassic().parseText(topicJson)
        def changeId = topicData.id
        if(changeId != null && changeId != "") {
            def commitJson = sh(script: "ssh -p ${port} ${url} gerrit query --format=JSON --current-patch-set ${changeId}", returnStdout: true).trim()
            def commitData = new JsonSlurperClassic().parseText(commitJson)
            def commitId = commitData.currentPatchSet.revision
            if(commitId != null && commitId != "") {
                return commitId
            }
        }
    }
    return "master"
}

/**
 * Retrieve commitid for a specific branch or refspec
 * Useful for Manual triggers when GERRIT_PATCHSET_REVISION is not defined
 *
 * @param url Git url
 * @param branch branchname or refspec
 * @return commitHash
 */
def getVersion(String url, String branch) {
        def cmd = "git ls-remote $url $branch | cut -f1"
        return sh(returnStdout: true, script: cmd).trim()
}

/**
 * Given Jenkins credentials, Retrieve commitid for a specific branch or refspec
 * Useful for Manual triggers when GERRIT_PATCHSET_REVISION is not defined
 *
 * @param url Git url
 * @param branch branchname or refspec
 * @param creds jenkins SSH credentials ID
 * @return commitHash
 */
def getVersion(String url, String branch, String creds) {
    withCredentials([sshUserPrivateKey(credentialsId: creds,
                                       keyFileVariable: 'SSH_KEY')]) {
        // wrapper for custom git ssh key
        // ssh -i $SSH_KEY $@
        def filewrapper = "/usr/bin/git-ssh-wrapper"
        if (!fileExists(filewrapper)) {
            sh """cat << EOF | sudo tee -a $filewrapper
#!/bin/bash
ssh -i $SSH_KEY \\\$@
EOF"""
            sh "sudo chmod a+x $filewrapper"
        }
        withEnv(["GIT_SSH=$filewrapper"]) {
            sh "ssh-keyscan -p ${INTERNAL_GERRIT_PORT} ${INTERNAL_GERRIT_URL} | tee -a ~/.ssh/known_hosts"
            def cmd = "git ls-remote $url $branch | cut -f1"
            return sh(returnStdout: true, script: cmd).trim()
        }
    }
}

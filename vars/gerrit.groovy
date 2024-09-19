import groovy.json.JsonSlurperClassic

import groovy.lang.*
import com.att.nccicd.config.conf as config

private void runGit(String command) {
    String credsName = sh(script: 'git config --local --get core.credentialsName', returnStdout: true)
    if (credsName) {
        // provide ssh key only when actual command is being executed
        // and remove right after to protect it.
        withCredentials([sshUserPrivateKey(credentialsId: credsName, keyFileVariable: 'SSH_KEY')]) {
            sh(script: command)
        }
    } else {
        sh(script: command)
    }
}

/**
 * Clones git repository
 *
 * @param url - url of the repository
 * @param refspec - refspec to clone
 * @param shallow - perform shallow clone or not
 * @param creds - credentials for repository
 * @param tarhetDirectory - target directory for cloned data
 * @param localBranch - local git branch to clone into
 * @param gitCmd - override default GIT_SSH_COMMAND variable
 */
def cloneRepository(args) {
    if (!args.url || !args.refspec) {
        throw new IllegalArgumentException("Gerrit cloneRepository: 'url' and 'refspec' parameters are mandatory")
    }

    if (args.creds && args.gitCmd) {
        throw new IllegalArgumentException("Gerrit cloneRepository: 'creds' and 'gitCmd' parameters should not be defined at the same time")
    }

    // remove remote name from branch and refspec
    args.refspec = args.refspec.trim().replaceFirst(/(?i)^origin\//, '')
    if (args.localBranch) {
        args.localBranch = args.localBranch.trim().replaceFirst(/(?i)^origin\//, '')
    }

    // by default there is a global var defining whether clone should be shallow or not
    args.shallow = (args.shallow == null) ? checkShallowVariable() : args.shallow

    // by default last component of the repo url is considered a name of the local folder.
    // if repo has '/' in it's name - only string after it will be used as a folder name
    // this is consistent with 'git clone' behavior
    args.targetDirectory = args.targetDirectory ?: new URI(args.url).path.split('/').last()

    // make folder for cloned repo (and fail if it exists)
    sh "mkdir \"${args.targetDirectory}\""
    dir(args.targetDirectory) {
        // init local repo
        sh "git init"

        // set custom ssh command for this repo
        if (args.creds) {
            sh "git config --local --add core.credentialsName '${args.creds}'"
            sh "git config --local --add core.sshCommand 'ssh -i \"\$SSH_KEY\" -o UserKnownHostsFile=\"${_setupKnownHosts()}\" -o StrictHostKeyChecking=yes'"
        } else if (args.gitCmd) {
            // use double quotes in case if provided cmd has shell var substitutions
            sh "git config --local --add core.sshCommand \"${args.gitCmd}\""
        } else {
            sh "git config --local --add core.sshCommand 'ssh -o UserKnownHostsFile=\"${_setupKnownHosts()}\" -o StrictHostKeyChecking=yes'"
        }

        // setup remote
        sh "git config remote.origin.url \"${args.url}\""

        // fetch & checkout
        runGit "git fetch ${args.shallow ? "--depth=${env.SHALLOW_DEPTH ?: 2}" : ''} ${env.GIT_OPTIONS ?: '-q'} origin ${args.refspec} && git checkout FETCH_HEAD ${args.localBranch ? "-b \"${args.localBranch}\"" : ''}"
    }
}

/**
 * Clones downstream git repository
 *
 * @param repo - name of the repository
 * @param refspec - refspec to clone
 * @param shallow - perform shallow clone or not
 * @param tarhetDirectory - target directory for cloned data
 * @param localBranch - local git branch to clone into
 * @param gitCmd - override default GIT_SSH_COMMAND variable
 */
def cloneDownstream(args) {
    if (!args.repo || !args.refspec) {
        throw new IllegalArgumentException("Gerrit cloneDownstream: 'repo' and 'refspec' parameters are mandatory")
    }
    // it uses "INTERNAL_GERRIT_SSH" instead of "conf.GERRIT_URL"
    // because different jenkins servers (from different corridors) have different git servers to work with
    // but it uses "conf.JENKINS_GERRIT_MTN5_CRED_ID" because the name of credentials
    // is the same on all corridors
    cloneRepository url: "${INTERNAL_GERRIT_SSH}/${args.repo}",
                    refspec: args.refspec,
                    shallow: args.shallow,
                    creds: new config(env).CONF.JENKINS_GERRIT_MTN5_CRED_ID,
                    targetDirectory: args.targetDirectory,
                    localBranch: args.localBranch,
                    gitCmd : args.gitCmd
}

def _setupKnownHosts() {
    knownHostsFile = sh(script: 'mktemp /tmp/tmp.ssh-XXXXXXXXX', returnStdout: true).trim()
    if (env.KNOWN_HOSTS) {
        sh "set +x; echo \"${KNOWN_HOSTS}\" > ${knownHostsFile}"
    }
    echo "Known hosts file ${knownHostsFile} was updated."
    return knownHostsFile
}

def checkShallowVariable() {
    shallowEnabled = env.SHALLOW_CLONE != null ? env.SHALLOW_CLONE : true
    return shallowEnabled.toBoolean()
}

/**
 * Rebase the local checkout with refspec
 *
 * @param refspec "xxxx/master" or other refspec
*/
def rebase(refspec='master') {
    runGit """git config user.email "jenkins@cicd"
              git config user.name "Jenkins"
              git pull --rebase origin "$refspec" """
}

/**
 * Merge 'refspec' revision into current code
 * Fast-forward merge (with no merge commit)
 * Is used if possible
 *
 * @param refspec "xxxx/master" or other refspec
*/
def merge(refspec='master') {
    runGit """
        git config user.email "jenkins@cicd"
        git config user.name "Jenkins"
        git fetch origin "$refspec"
        git merge --ff "\$(git rev-parse FETCH_HEAD)"
    """
}

/**
 * Retrieves the commit identifier of an "open" Gerrit patchset,
 * with a given topic set. Especially useful to get cross-repo
 * dependencies
 *
 * @param repo The repository to search for an "open" patchset with a given topic
 * @param url The url of the Gerrit to check against; ssh user included - e.g. "abc123@gerrit.foo.bar"
 * @param port The port Gerrit is running on
 * @param creds The Jenkins SSH credentials ID
 * @return commit The commitId of the "open" patchset with a given topic. If said PS doesn't exist, "master".
 */
def getTopicCommitId(repo, url, port, creds) {
    def revision = "master"
    withCredentials([sshUserPrivateKey(credentialsId: creds,
            keyFileVariable: 'SSH_KEY')]) {
        // If triggering repo includes a topic
        if(GERRIT_TOPIC != null && GERRIT_TOPIC != "") {
            def topicJson = sh(script: "ssh -i ${SSH_KEY} -p ${port} ${url} gerrit query --format=JSON topic:${GERRIT_TOPIC} status:open project:${repo}", returnStdout: true).trim()
            def topicData = new JsonSlurperClassic().parseText(topicJson)
            def changeId = topicData.id
            if(changeId != null && changeId != "") {
                def commitJson = sh(script: "ssh -i ${SSH_KEY} -p ${port} ${url} gerrit query --format=JSON --current-patch-set ${changeId}", returnStdout: true).trim()
                def commitData = new JsonSlurperClassic().parseText(commitJson)
                def commitId = commitData.currentPatchSet.revision
                if(commitId != null && commitId != "") {
                    revision = commitId
                }
            }
        }
    }
    return revision
}

/**
 * Retrieves the commit details of an "open" Gerrit patchset,
 * with a given topic set. Especially useful to get cross-repo
 * dependencies
 *
 * @param repo The repository to search for an "open" patchset with a given topic
 * @param url The url of the Gerrit to check against; ssh user included - e.g. "abc123@gerrit.foo.bar"
 * @param port The port Gerrit is running on
 * @param creds The Jenkins SSH credentials ID
 *
 * @return the list of commits of the "open" patchset with a given topic. If said PS doesn't exist, empty list "[]".
 */
def getTopicCommitInfo(repo, url, port, creds) {
    def commits = []
    def jsonList = []
    withCredentials([sshUserPrivateKey(credentialsId: creds,
            keyFileVariable: 'SSH_KEY')]) {
        // If triggering repo includes a topic
        if(GERRIT_TOPIC != null && GERRIT_TOPIC != "") {
            def topicJson = sh(script: "ssh -i ${SSH_KEY} -p ${port} ${url} gerrit query \
                                        --format=JSON topic:${GERRIT_TOPIC} \
                                        status:open project:${repo}", returnStdout: true).trim()
            jsonList = topicJson.tokenize("\n")

            // Return empty list if no commits for topic
            if (jsonList.isEmpty()) {
                return commits
            }

            jsonList.each({
                def topicData = new JsonSlurperClassic().parseText(it)
                // skip stats from result
                if (topicData.type != 'stats') {
                    def changeId = topicData.id
                    if(changeId != null && changeId != "") {
                        def commitJson = sh(script: "ssh -i ${SSH_KEY} -p ${port} ${url} \
                                            gerrit query --format=JSON --current-patch-set \
                                            ${changeId}", returnStdout: true).trim()
                        def commitData = new JsonSlurperClassic().parseText(commitJson)
                        topicData['commitInfo'] = commitData
                        commits.push(topicData)
                    }
                }
            })
        }
    }
    return commits
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
            sh """cat << EOF | sudo tee $filewrapper
#!/bin/bash
ssh -i \\\$SSH_KEY \\\$@
EOF"""
            sh "sudo chmod a+x $filewrapper"
        }
        withEnv(["GIT_SSH=$filewrapper"]) {
            def cmd = "git ls-remote $url $branch | cut -f1"
            return sh(returnStdout: true, script: cmd).trim()
        }
    }
}

/**
 * Submit a patchset into the specified branch.
 *
 * @param credentials Gerrit credentials to submit a patchset
 * @param userEmail Email of Jenkins user that triggered the job
 * @param userName BUILD_USER that triggered the Jenkins job
 * @param gerritUrl "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param repoName name of the repository being pushed to
 * @param refspec "xxxx/master" or other refspec
 */
def submitPatchset(credentials, userEmail, userName, commitMessage, gerritUrl, repoName, refspec = "refs/for/master", workDir=null) {
    workDir = workDir != null ? workDir: repoName
    knownHostsFile = _setupKnownHosts()
    sshParams = "-i \${SSH_KEY} -o UserKnownHostsFile=${knownHostsFile} -o StrictHostKeyChecking=yes"
    withCredentials([sshUserPrivateKey(credentialsId: credentials,
        keyFileVariable: 'SSH_KEY')]) {
        dir(workDir) {
            sh """
                 export GIT_SSH_COMMAND="ssh ${sshParams}"
                 git config user.email '${userEmail}'
                 git config user.name '${userName}'
                 git config --global push.default matching
                 git status
                 git add .
                 git commit -m "${commitMessage}"
                 scp ${sshParams} -p -P 29418 ${gerritUrl}:hooks/commit-msg .git/hooks
                 git commit --amend --no-edit
                 git push -v ssh://${gerritUrl}:29418/${repoName} HEAD:${refspec}
               """
        }
    }
    sh "rm ${knownHostsFile}"
}

/**
 * Submit a patchset with topic into the specified branch. Must be using git 2.10.2 or greater.
 *
 * @param credentials Gerrit credentials to submit a patchset
 * @param userEmail Email of Jenkins user that triggered the job
 * @param userName BUILD_USER that triggered the Jenkins job
 * @param gerritUrl "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param repoName name of the repository being pushed to
 * @param gerritTopic topic for submitted patchset
 * @param refspec "xxxx/master" or other refspec
 */
def submitPatchsetWithTopic(credentials, userEmail, userName, commitMessage, gerritUrl, repoName, refspec = "refs/for/master", gerritTopic = "", workDir=null) {
    workDir = workDir != null ? workDir: repoName
    knownHostsFile = _setupKnownHosts()
    sshParams = "-i \${SSH_KEY} -o UserKnownHostsFile=${knownHostsFile} -o StrictHostKeyChecking=yes"
    withCredentials([sshUserPrivateKey(credentialsId: credentials,
        keyFileVariable: 'SSH_KEY')]) {
        dir(workDir) {
            sh """
                 export GIT_SSH_COMMAND="ssh ${sshParams}"
                 git config user.email '${userEmail}'
                 git config user.name '${userName}'
                 git config --global push.default matching
                 git status
                 git add .
                 git commit -m "${commitMessage}"
                 scp ${sshParams} -p -P 29418 ${gerritUrl}:hooks/commit-msg .git/hooks
                 git commit --amend --no-edit
                 git push -v ssh://${gerritUrl}:29418/${repoName} HEAD:${refspec} -o topic=${gerritTopic}
           """
        }
    }
    sh "rm ${knownHostsFile}"
}

/**
 * Amend an existing patchset that has been cloned to branch.
 *
 * @param credentials Gerrit credentials to submit a patchset
 * @param userEmail Email of Jenkins user that triggered the job
 * @param userName BUILD_USER that triggered the Jenkins job
 * @param gerritUrl "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param repoName name of the repository being pushed to
 * @param refspec "xxxx/master" or other refspec
 */
def amendPatchset(credentials, userEmail, userName, gerritUrl, repoName, refspec = "refs/for/master", workDir=null) {
    workDir = workDir != null ? workDir: repoName
    knownHostsFile = _setupKnownHosts()
    withCredentials([sshUserPrivateKey(credentialsId: credentials,
        keyFileVariable: 'SSH_KEY')]) {
        dir(workDir) {
            sh """
                 export GIT_SSH_COMMAND="ssh -i \${SSH_KEY} -o UserKnownHostsFile=${knownHostsFile} -o StrictHostKeyChecking=yes"
                 git config user.email '${userEmail}'
                 git config user.name '${userName}'
                 git config --global push.default matching
                 git status
                 git add .
                 git commit --amend --no-edit
                 git push -v ssh://${gerritUrl}:29418/${repoName} HEAD:${refspec}
               """
        }
    }
    sh "rm ${knownHostsFile}"
}

/**
 * Amend an existing patchset that has been cloned to branch. Must be using git 2.10.2 or greater.
 *
 * @param credentials Gerrit credentials to submit a patchset
 * @param userEmail Email of Jenkins user that triggered the job
 * @param userName BUILD_USER that triggered the Jenkins job
 * @param gerritUrl "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param repoName name of the repository being pushed to
 * @param gerritTopic topic for submitted patchset
 * @param refspec "xxxx/master" or other refspec
 */
def amendPatchsetWithTopic(credentials, userEmail, userName, gerritUrl, repoName, refspec = "refs/for/master", gerritTopic = "", workDir=null) {
    workDir = workDir != null ? workDir: repoName
    knownHostsFile = _setupKnownHosts()
    withCredentials([sshUserPrivateKey(credentialsId: credentials,
        keyFileVariable: 'SSH_KEY')]) {
        dir(workDir) {
            sh """
                 export GIT_SSH_COMMAND="ssh -i \${SSH_KEY} -o UserKnownHostsFile=${knownHostsFile} -o StrictHostKeyChecking=yes"
                 git config user.email '${userEmail}'
                 git config user.name '${userName}'
                 git config --global push.default matching
                 git status
                 git add .
                 git commit --amend --no-edit
                 git push -v ssh://${gerritUrl}:29418/${repoName} HEAD:${refspec} -o topic=${gerritTopic}
               """
        }
    }
    sh "rm ${knownHostsFile}"
}

/**
 * Given Jenkins credentials, Retrieve commit details for a given repo path
 *
 * @param repo_path diretory where git repo is cloned
 * @return latestCommitInfo
 */
def getLocalRepoVersion(repo_path) {
    // get git log information from the given directory path
    // H => commit hash, %d => ref names, %s => subject , %ce => committer email
    def cmd = "cd  ${repo_path} && git log -1  --pretty=format:%H::::%d::::%s::::%ce"
    def git_log =  sh(returnStdout: true, script: cmd).trim()
    return git_log.split('::::')
}


/**
 * Get Commit diff for a given repo path and commit ID
 *
 * @param repo_path directory where git repo is cloned
 * @param commit_id to get diff
 */
def getCommitDiff(repo_path, commit_id) {
    def cmd = "cd  ${repo_path} && git show ${commit_id}"
    def git_diff =  sh(returnStdout: true, script: cmd).trim()
    return git_diff
}

//region Deprecated clone/fetch functions. Please use cloneRepository/cloneDownstream - they can replace any of the methods below.

@Deprecated
def clone(String url, String refspec) {
    // Usage example: gerrit.clone("gerrit url", "origin/master")
    // clone refspec: gerrit.clone("gerrit url", "${env.GERRIT_REFSPEC}")
    cloneRepository(url: url, refspec: refspec, shallow: false)
}

/**
 * Given Jenkins credentials, clones Git repository via SSH
 *
 * @param url "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param refspec "xxxx/master" or other refspec
 * @param creds jenkins SSH credentials ID
 */
@Deprecated
def clone(String url, String refspec, String creds) {
    // Usage example: gerrit.clone("ssh://${GERRIT_HOST}/${GERRIT_PROJECT}", '*/master', "jenkins-gerrit-ssh-creds")
    cloneRepository(url: url, refspec: refspec, creds: creds, shallow: false)
}

@Deprecated
def _cloneShallowCmd(url, branch, gitSshCommand="") {
    depth = env.SHALLOW_DEPTH ? env.SHALLOW_DEPTH : 2
    opts = env.GIT_OPTIONS ? env.GIT_OPTIONS : '-q'
    sh """
      set -x
      ${gitSshCommand}
      git init
      git config remote.origin.url ${url}
      git fetch --depth=${depth} ${url} ${branch} && git checkout FETCH_HEAD
      if [ \${?} -eq 128 ]; then
        git fetch ${url} ${opts} && git checkout ${branch}
      fi
    """
}

@Deprecated
def _cloneShallow(url, branch, targetDirectory, gitSshCommand="") {
    if ( targetDirectory ) {
        sh "mkdir -p ${targetDirectory}"
        dir("${targetDirectory}") {
            _cloneShallowCmd(url, branch, gitSshCommand)
        }
    } else {
        _cloneShallowCmd(url, branch, gitSshCommand)
    }
}

@Deprecated
def _clone(url, branch, targetDirectory, refspec='${GERRIT_REFSPEC}') {
    cloneRepository(url: url, refspec: branch, localBranch: 'jenkins', targetDirectory: targetDirectory, shallow: false)
}

@Deprecated
def _cloneWithCredsShallow(url, branch, targetDirectory, creds, refspec) {
    knownHostsFile = _setupKnownHosts()
    withCredentials([sshUserPrivateKey(credentialsId: creds,
        keyFileVariable: 'SSH_KEY')]) {
        gitSshCommand = "export GIT_SSH_COMMAND=\"ssh -i \${SSH_KEY} -o UserKnownHostsFile=${knownHostsFile} -o StrictHostKeyChecking=yes\""
        _cloneShallow(url, branch, targetDirectory, gitSshCommand)
    }
    sh "rm ${knownHostsFile}"
}

@Deprecated
def _cloneWithCreds(url, branch, targetDirectory, creds, refspec) {
    cloneRepository(url: url, refspec: branch, localBranch: 'jenkins', targetDirectory: targetDirectory, creds: creds, shallow: false)
}

@Deprecated
def cloneToBranch(String url, String refspec, String targetDirectory){
//This method is used so that we can checkout the patchset to a local
//branch and then rebase it locally with the current master before we build and test
    shallowEnabled = checkShallowVariable()
    if (shallowEnabled) {
        _cloneShallow(url, refspec, targetDirectory)
    } else {
        _clone(url, refspec, targetDirectory)
    }
}

/**
 * Given Jenkins credentials, clones Git repository via SSH to the
 * target directory to local branch using a specified refs spec instead of
 * the value passed from the Gerrit Trigger.
 *
 * @param url "ssh://${GERRIT_HOST}/${GERRIT_PROJECT}" string
 * @param refspec "xxxx/master" or other refspec
 * @param targetDirectory local directory where to clone repo
 * @param creds jenkins SSH credentials ID
 * @param gerritRefspec Overridden refspec value
 */
@Deprecated
def cloneToBranch(String url, String refspec, String targetDirectory, String creds, String gerritRefspec) {
    shallowEnabled = checkShallowVariable()
    if (shallowEnabled) {
        _cloneWithCredsShallow(url, refspec, targetDirectory, creds, gerritRefspec)
    } else {
        _cloneWithCreds(url, refspec, targetDirectory, creds, gerritRefspec)
    }
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
@Deprecated
def cloneToBranch(String url, String refspec, String targetDirectory, String creds){
    shallowEnabled = checkShallowVariable()
    if (shallowEnabled) {
        _cloneWithCredsShallow(url, refspec, targetDirectory, creds, '${GERRIT_REFSPEC}')
    } else {
        _cloneWithCreds(url, refspec, targetDirectory, creds, '${GERRIT_REFSPEC}')
    }
}

//Replace clone and rebase methods
@Deprecated
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

@Deprecated
def cloneProject(String url, String branch, String refspec, String targetDirectory){
//This method is used so that we can checkout different project
//from any patchset in different pipelines
    shallowEnabled = checkShallowVariable()
    if (shallowEnabled) {
        _cloneShallow(url, branch, targetDirectory)
    } else {
        _clone(url, branch, targetDirectory, refspec)
    }
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
@Deprecated
def cloneProject(String url, String branch, String refspec, String targetDirectory, String creds){
    shallowEnabled = checkShallowVariable()
    if (shallowEnabled) {
        _cloneWithCredsShallow(url, branch, targetDirectory, creds, refspec)
    } else {
        _cloneWithCreds(url, branch, targetDirectory, creds, refspec)
    }
}

//endregion

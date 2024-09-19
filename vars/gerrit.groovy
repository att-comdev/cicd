import groovy.json.JsonSlurperClassic

def cloneRepository(args) {
    //String url, String refspec, Boolean shallow = null, String creds = null, String gitCmd = null, String targetDirectory = null, String localBranch = null
    args.shallow = args.shallow ?: checkShallowVariable()

    sh "cat ${_setupKnownHosts()}"

    def runGit = {
        sh """
            set -x
            $it
            git init
            git config remote.origin.url ${args.url}
            git fetch ${args.shallow ? "--depth=${env.SHALLOW_DEPTH ?: 2}" : ''} ${env.GIT_OPTIONS ?: '-q'} origin ${args.refspec} && git checkout FETCH_HEAD ${args.localBranch ? "-b \"${args.localBranch}\"" : ''}
        """
    }

    if (args.creds && args.gitCmd) {
        error("Gerrit cloneRepository: 'creds' and 'gitCmd' parameters should not be defined at the same time")
    }

    if (args.creds) {
        withCredentials([sshUserPrivateKey(credentialsId: args.creds, keyFileVariable: 'SSH_KEY')])
        {
            runGit("export GIT_SSH_COMMAND=\"ssh -i $SSH_KEY -o UserKnownHostsFile=${_setupKnownHosts()} -o StrictHostKeyChecking=yes\"")
        }
    } else if (args.gitCmd) {
        runGit(args.gitCmd)
    } else {
        runGit("export GIT_SSH_COMMAND=\"ssh -o UserKnownHostsFile=${_setupKnownHosts()} -o StrictHostKeyChecking=yes\"")
    }
}

@Deprecated
def clone(String url, String refspec) {
    cloneRepository(url: url, refspec: GERRIT_REFSPEC)
}

@Deprecated
def clone(String url, String refspec, String creds) {
    cloneRepository(url: url, refspec: GERRIT_REFSPEC, creds: creds)
}

@Deprecated
def _cloneShallowCmd(url, branch, gitSshCommand='') {
    cloneRepository(url: url, refspec: branch, gitCmd: gitSshCommand, shallow: true)
}

@Deprecated
def _cloneShallow(url, branch, targetDirectory, gitSshCommand='') {
    cloneRepository(url: url, refspec: branch, gitCmd: gitSshCommand, shallow: true, targetDirectory: targetDirectory)
}

@Deprecated
def _clone(url, branch, targetDirectory, refspec=null) {
    cloneRepository(url: url, refspec: refspec ?: GERRIT_REFSPEC, targetDirectory: targetDirectory, localBranch: 'jenkins')
}

@Deprecated
def _cloneWithCredsShallow(url, branch, targetDirectory, creds, refspec) {
    cloneRepository(url: url, refspec: refspec, shallow: true, targetDirectory: targetDirectory, creds: creds)
}

@Deprecated
def _cloneWithCreds(url, branch, targetDirectory, creds, refspec) {
    cloneRepository(url: url, refspec: refspec, targetDirectory: targetDirectory, creds: creds, localBranch: 'jenkins')
}

@Deprecated
def cloneToBranch(String url, String refspec, String targetDirectory) {
    cloneRepository(url: url, refspec: refspec, targetDirectory: targetDirectory)
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
    cloneRepository(url: url, refspec: gerritRefspec, targetDirectory: targetDirectory, creds: creds)
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
def cloneToBranch(String url, String refspec, String targetDirectory, String creds) {
    cloneRepository(url: url, refspec: GERRIT_REFSPEC, targetDirectory: targetDirectory, creds: creds)
}

@Deprecated
def cloneProject(String url, String branch, String refspec, String targetDirectory) {
    //This method is used so that we can checkout different project
    //from any patchset in different pipelines
    shallowEnabled = checkShallowVariable()
    if (shallowEnabled) {
        cloneRepository(url: url, refspec: branch, targetDirectory = targetDirectory, shallow = true)
    } else {
        cloneRepository(url: url, refspec: refspec, targetDirectory = targetDirectory, shallow = false)
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
def cloneProject(String url, String branch, String refspec, String targetDirectory, String creds) {
    cloneRepository(url: url, refspec: refspec, targetDirectory = targetDirectory, creds = creds)
}

//Replace clone and rebase methods
@Deprecated
def checkout(String revision, String branchToClone, String refspec, String targetDirectory) {
    if (revision) {
        IMAGE_TAG = revision
    }
    cloneToBranch(branchToClone, refspec, targetDirectory)
    if (!revision) {
        dir(env.WORKSPACE + '/' + targetDirectory) {
            rebase()
        }
    }
}

def _setupKnownHosts() {
    def lines = []

    // Get host keys from git plugin. This can be used as a single source of host keys to avoid duplicated config.
    def gitPluginKeys = org.jenkinsci.plugins.gitclient.GitHostKeyVerificationConfiguration.get()
        ?.sshHostKeyVerificationStrategy
        ?.getApprovedHostKeys()

    if (gitPluginKeys) {
        lines += gitPluginKeys.readLines()
    }

    // KNOWN_HOSTS env var is still supported. But we should avoid duplicating data in it.
    if (env.KNOWN_HOSTS) {
        lines += env.KNOWN_HOSTS.readLines()
    }

    knownHostsFile = sh(script: 'mktemp /tmp/tmp.ssh-XXXXXXXXX', returnStdout: true).trim()
    writeFile(file: knownHostsFile, text: lines.join('\n'))

    println "Known hosts file ${knownHostsFile} was updated."

    sh "cat $knownHostsFile"

    return knownHostsFile
}

def checkShallowVariable() {
    shallowEnabled = env.SHALLOW_CLONE != null ? env.SHALLOW_CLONE : true
    return shallowEnabled.toBoolean()
}

def rebase() {
    //This method will rebase the local checkout with master and then continue build, tests, etc
    sh '''git config user.email "airship.jenkins@gmail.com"
          git config user.name "Jenkins"
          git pull --rebase origin master'''
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
    def revision = 'master'
    withCredentials([sshUserPrivateKey(credentialsId: creds, keyFileVariable: 'SSH_KEY')]) {
        // If triggering repo includes a topic
        if (GERRIT_TOPIC != null && GERRIT_TOPIC != '') {
            def topicJson = sh(script: "ssh -i ${SSH_KEY} -p ${port} ${url} gerrit query --format=JSON topic:${GERRIT_TOPIC} status:open project:${repo}", returnStdout: true).trim()
            def topicData = new JsonSlurperClassic().parseText(topicJson)
            def changeId = topicData.id
            if (changeId != null && changeId != '') {
                def commitJson = sh(script: "ssh -i ${SSH_KEY} -p ${port} ${url} gerrit query --format=JSON --current-patch-set ${changeId}", returnStdout: true).trim()
                def commitData = new JsonSlurperClassic().parseText(commitJson)
                def commitId = commitData.currentPatchSet.revision
                if (commitId != null && commitId != '') {
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
        if (GERRIT_TOPIC != null && GERRIT_TOPIC != '') {
            def topicJson = sh(script: "ssh -i ${SSH_KEY} -p ${port} ${url} gerrit query \
                                        --format=JSON topic:${GERRIT_TOPIC} \
                                        status:open project:${repo}", returnStdout: true).trim()
            jsonList = topicJson.tokenize('\n')

            // Return empty list if no commits for topic
            if (jsonList.isEmpty()) {
                return commits
            }

            jsonList.each({
                def topicData = new JsonSlurperClassic().parseText(it)
                // skip stats from result
                if (topicData.type != 'stats') {
                    def changeId = topicData.id
                    if (changeId != null && changeId != '') {
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
        def filewrapper = '/usr/bin/git-ssh-wrapper'
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
def submitPatchset(credentials, userEmail, userName, commitMessage, gerritUrl, repoName, refspec = 'refs/for/master', workDir=null) {
    workDir = workDir != null ? workDir : repoName
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
def submitPatchsetWithTopic(credentials, userEmail, userName, commitMessage, gerritUrl, repoName, refspec = 'refs/for/master', gerritTopic = '', workDir=null) {
    workDir = workDir != null ? workDir : repoName
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
def amendPatchset(credentials, userEmail, userName, gerritUrl, repoName, refspec = 'refs/for/master', workDir=null) {
    workDir = workDir != null ? workDir : repoName
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
def amendPatchsetWithTopic(credentials, userEmail, userName, gerritUrl, repoName, refspec = 'refs/for/master', gerritTopic = '', workDir=null) {
    workDir = workDir != null ? workDir : repoName
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
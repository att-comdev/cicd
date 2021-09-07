import groovy.json.JsonSlurperClassic
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import hudson.triggers.TimerTrigger
import hudson.model.Cause.UserIdCause

if (!env.GERRIT_BRANCH) {
    GERRIT_EVENT_TYPE = "Manual"
}
currentBuild.displayName = "#${BUILD_NUMBER} ${GERRIT_EVENT_TYPE}"
if (currentBuild.rawBuild.getCause(hudson.triggers.TimerTrigger.TimerTriggerCause)) {
 currentBuild.displayName = "#${BUILD_NUMBER} Timer (${GERRIT_REFSPEC})"
}

def cloneref = {
    sh "sudo rm -rf airshipctl || true"
    sh "git clone https://review.opendev.org/airship/airshipctl"
    sh "git clone -b master https://review.opendev.org/airship/treasuremap"
    envVars = env.getEnvironment()
    println envVars.containsKey("GERRIT_REFSPEC")
    if (envVars.containsKey("GERRIT_REFSPEC")) {
        sh "cd treasuremap && git fetch https://review.opendev.org/airship/treasuremap $GERRIT_REFSPEC && git checkout FETCH_HEAD && git checkout -b gerrit_current"
    }
    if(envVars.containsKey("AIRSHIPCTL_REF")) {
        sh "cd airshipctl && git fetch https://review.opendev.org/airship/airshipctl $AIRSHIPCTL_REF && git checkout FETCH_HEAD && git checkout -b gerrit_current"
    }
}


def collect_log = {
    println("Colelcting Log")
    sh "cd airshipctl &&  tools/gate/99_collect_logs.sh"
    sh "sudo tar -cvzf /tmp/treasuremap_build_${BUILD_NUMBER}-logs.tgz -C /tmp/logs ."
    sh "sudo apt install -y sshpass"

    withCredentials([usernamePassword(credentialsId: "jenkins_master",
        usernameVariable: "USER",
        passwordVariable: "PASSWORD")]) {
        sh "sshpass -p ${PASSWORD} scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no  /tmp/treasuremap_build_${BUILD_NUMBER}-logs.tgz ${USER}@10.254.125.160:/tmp"
    }

    withCredentials([usernamePassword(credentialsId: "jenkins_master",
          usernameVariable: "USER",
          passwordVariable: "PASSWORD")]) {
          sh "sshpass -p ${PASSWORD} ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no  ${USER}@10.254.125.160 sudo cp /tmp/treasuremap_build_${BUILD_NUMBER}-logs.tgz /mnt/jenkins-data/userContent/"
    }
    println "The log location: https://jenkins.nc.opensource.att.com/userContent/treasuremap_build_${BUILD_NUMBER}-logs.tgz"
}

def _setupKnownHosts() {
    knownHostsFile = sh(script: 'mktemp /tmp/tmp.ssh-XXXXXXXXX', returnStdout: true).trim()
    if (env.KNOWN_HOSTS) {
        sh "set +x; echo \"${KNOWN_HOSTS}\" > ${knownHostsFile}"
    }
    echo "Known hosts file ${knownHostsFile} was updated."
    return knownHostsFile

}

def submit_patchset(credentials, userEmail, userName, commitMessage, gerritUrl, repoName, refspec = "refs/for/master", workDir=null) {
    workDir = workDir != null ? workDir: repoName
    knownHostsFile = _setupKnownHosts()
    sshParams = "-i \${SSH_KEY} -o UserKnownHostsFile=${knownHostsFile} -o StrictHostKeyChecking=no"
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

uplift_airshipctl_version = {
    AIRSHIPCTL_COMMIT_VERSION = sh (
        script: 'cd airshipctl && git rev-parse HEAD',
        returnStdout: true
    ).trim()
    echo "Airshipctl version: ${AIRSHIPCTL_COMMIT_VERSION} is compatibe with Treasuremap master"

    echo "${WORKSPACE}/airship/treasuremap"
    dir('treasuremap') {
        // Update airshictl_ref in treasuremap and push the change to gerritUrl
        GERRIT_SSH_KEY = 'GERRIT_SSH_KEY'
        GERRIT_USER_NAME = 'airship2ci'
        GERRIT_USER_EMAIL = 'airship2ci@gmail.com'
        def commitMessage = "[auto-uplift]: Update airshipctl ref to latest"
        def gerritUrl = "${GERRIT_USER_NAME}@review.opendev.org"
        def repoName = "airship/treasuremap"
        sh("""
            sed -i "s/^\\(\\s*AIRSHIPCTL_REF\\s*:\\s*\\).*/\\1$AIRSHIPCTL_COMMIT_VERSION/" zuul.d/projects.yaml
        """)
        sh "git diff"
        submit_patchset(GERRIT_SSH_KEY, "${GERRIT_USER_EMAIL}", "${GERRIT_USER_NAME}", commitMessage, gerritUrl, repoName, "refs/for/master", "${WORKSPACE}/treasuremap")
    }
}


def deploy = {

    stage('Clone Code') {
            sh "sudo rm -rf treasuremap || true"
            cloneref()
    }
    stage('Cleanup'){
           println("Cleanup packages, images")
           sh "sudo rm -rf ~/.airship || true"
           sh "sudo rm -rf /tmp/* || true"
           sh "cd airshipctl &&  tools/deployment/clean.sh || true "
    }
    stage('Setup') {
        println("Running setup")
        sh "cd treasuremap &&  tools/gate/00_setup.sh"
    }
    stage('build') {
            println("Running build")
        sh "cd treasuremap &&  tools/gate/10_build_gate.sh"
    }
    stage('install') {
            println("Running Deployment")
            try {
                println("Running tools/gate/20_run_gate_runner.sh")
                sh "cd treasuremap &&  tools/gate/20_run_gate_runner.sh"
                try {
                    print "Build Succeded. Start Log collection"
                    collect_log()
                } catch (error) {
                    print "Log collection Failed : ${error.getMessage()}"
                }
            } catch (FlowInterruptedException err) {
                print "Skip Log collect for aborted Jobs"
                throw err
            } catch (err){
                try {
                    print "Build Failed : ${err.getMessage()}. Start Log collection"
                    collect_log()
                } catch (error) {
                    print "Log collection Failed : ${error.getMessage()}"
                }
                throw err
            }
            uplift_airshipctl_version()
        }
}

//// main flow

try {
    node (label: NODE_LABEL){
        cleanWs()
                deploy()
            }
} catch (error) {
    print "Build failed: ${error.getMessage()}"
    currentBuild.result = 'FAILURE'

}

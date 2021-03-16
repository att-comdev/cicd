import groovy.json.JsonSlurperClassic
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import hudson.triggers.TimerTrigger
import hudson.model.Cause.UserIdCause

// Update Build Title to make it more readable and search to filter builds
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
	envVars = env.getEnvironment()
	println envVars.containsKey("GERRIT_REFSPEC")
	if (envVars.containsKey("GERRIT_REFSPEC")) {
		sh "cd airshipctl && git fetch https://review.opendev.org/airship/airshipctl $GERRIT_REFSPEC  && git checkout FETCH_HEAD && git checkout -b gerrit_current"
	}
}

def collect_log = {
	println("Colelcting Log")
	sh "cd airshipctl &&  tools/gate/99_collect_logs.sh"
	sh "sudo tar -cvzf /tmp/airshipctl_build_${BUILD_NUMBER}-logs.tgz -C /tmp/logs ."
	sh "sudo apt install -y sshpass"

	withCredentials([usernamePassword(credentialsId: "jenkins_master",
		usernameVariable: "USER",
		passwordVariable: "PASSWORD")]) {
		sh "sshpass -p ${PASSWORD} scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no  /tmp/airshipctl_build_${BUILD_NUMBER}-logs.tgz ${USER}@10.254.125.160:/tmp"
	}

	withCredentials([usernamePassword(credentialsId: "jenkins_master",
		  usernameVariable: "USER",
		  passwordVariable: "PASSWORD")]) {
		  sh "sshpass -p ${PASSWORD} ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no  ${USER}@10.254.125.160 sudo cp /tmp/airshipctl_build_${BUILD_NUMBER}-logs.tgz /mnt/jenkins-data/userContent/"
	}
	println "The log location: https://jenkins.nc.opensource.att.com/userContent/airshipctl_build_${BUILD_NUMBER}-logs.tgz"
}

def deploy_airship2 = {

	stage('Clone Code') {
		cloneref()
	}

	stage('Cleanup'){
	   println("Cleanup packages, images")
	   sh "cd airshipctl &&  tools/deployment/clean.sh || true "
	}
	stage('Setup') {
		println("Running setup")
		withEnv (["AIRSHIPCTL_WS=${WORKSPACE}/airshipctl"]) {
						sh " airshipctl/tools/gate/00_setup.sh"
		}
	}
	stage('build') {
		println("Running build")
		sh "cd airshipctl &&  tools/gate/10_build_gate.sh"
	}

	stage('install') {
		println("Running Deployment")
		try {
			sh "cd airshipctl &&  tools/gate/20_run_gate_runner.sh"
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
	}
}

//// main flow
try {
	node (label: NODE_LABEL){
		cleanWs()
		deploy_airship2()
	}
} catch (error) {
	print "Build failed: ${error.getMessage()}"
	currentBuild.result = 'FAILURE'
}

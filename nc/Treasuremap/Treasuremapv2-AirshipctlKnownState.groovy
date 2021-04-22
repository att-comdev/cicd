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
	sh "git clone -b v2.0 https://review.opendev.org/airship/treasuremap"
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
}

def trigger = {
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
	}
}

//// main flow

try {
	node (label: NODE_LABEL){
		cleanWs()
		trigger()
	}
} catch (error) {
	print "Build failed: ${error.getMessage()}"
	currentBuild.result = 'FAILURE'
}
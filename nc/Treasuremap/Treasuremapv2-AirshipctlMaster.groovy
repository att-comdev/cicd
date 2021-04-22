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
	sh "git clone -b v2 https://review.opendev.org/airship/treasuremap"
	envVars = env.getEnvironment()
	println envVars.containsKey("GERRIT_REFSPEC")
	if (envVars.containsKey("GERRIT_REFSPEC")) {
		sh "cd treasuremap && git fetch https://review.opendev.org/airship/treasuremap $GERRIT_REFSPEC && git checkout FETCH_HEAD && git checkout -b gerrit_current"
	}
	if(envVars.containsKey("AIRSHIPCTL_REF")) {
		sh "cd airshipctl && git fetch https://review.opendev.org/airship/airshipctl $AIRSHIPCTL_REF && git checkout FETCH_HEAD && git checkout -b gerrit_current"
	}
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
		withEnv (["AIRSHIPCTL_WS=${WORKSPACE}/airshipctl"]) {
			sh " airshipctl/tools/gate/00_setup.sh"
		}
	}
	stage('build') {
		println("Running build")
		sh "cd airshipctl &&  tools/gate/10_build_gate.sh"
	}
	stage('install kubectl, pip, yq ') {
		println("Running Deployment")
		sh "cd treasuremap && sudo -E tools/deployment/01_install_essentials.sh"
	}
	stage('create config ') {
		println("Running Deployment")
		withEnv (["AIRSHIP_CONFIG_MANIFEST_DIRECTORY=${WORKSPACE}"]) {
			sh "cd treasuremap && sudo -E tools/deployment/22_test_configs.sh"
		}
	}
	stage('Copy source to /tmp/default ') {
		println("Skip pull and copy source.")
		sh 'printenv'
		sh "sudo rm -rf /tmp/default"
		sh "sudo mkdir /tmp/default"
		sh "sudo cp -r treasuremap /tmp/default"
		sh "sudo cp -r airshipctl /tmp/default"
	}
	stage('generate secrets ') {
		println("Running Deployment")
		sh 'printenv'
		sh "cd treasuremap && sudo -E tools/deployment/23_generate_secrets.sh"
	}
	stage('Build images ') {
		println("Running Deployment")
		sh "cd treasuremap && sudo -E tools/deployment/24_build_images.sh"
	}
	stage('Deploy ephemeral') {
		println("Running Deployment")
		sh "cd treasuremap && sudo -E tools/deployment/25_deploy_ephemeral_node.sh"
	}
	stage('Deploy ephemeral capi') {
		println("Running Deployment")
		sh "cd treasuremap && sudo -E tools/deployment/26_deploy_capi_ephemeral_node.sh"
	}
	stage('Deploy controlplane') {
		println("Running Deployment")
		sh "cd treasuremap && sudo -E tools/deployment/30_deploy_controlplane.sh"
	}
	stage('Deploy initinfra') {
		println("Running Deployment")
		sh "cd treasuremap && sudo -E tools/deployment/31_deploy_initinfra_target_node.sh"
	}
	stage('Target infra init') {
		println("Running Deployment")
		sh "cd treasuremap && sudo -E tools/deployment/32_cluster_init_target_node.sh"
	}
	stage('Move to target cluster') {
		println("Deploy worker load")
		sh "cd treasuremap && sudo -E tools/deployment/33_cluster_move_target_node.sh"
	}
	stage('Deploy worker node') {
		println("Deploy worker load")
		sh "cd treasuremap && sudo -E tools/deployment/34_deploy_worker_node.sh"
	}
	stage('Deploy workloads') {
		println("Deploy worker load")
		sh "cd treasuremap && sudo -E tools/deployment/35_deploy_workload.sh"
	}
	stage('Verify HWCC') {
		println("Verify HWCC Profiles")
		sh "cd treasuremap && sudo -E tools/deployment/36_verify_hwcc_profiles.sh"
	}
}

//// main flow

try {
	node (label: 'airship'){
		cleanWs()
		trigger()
	}
} catch (error) {
	print "Build failed: ${error.getMessage()}"
	currentBuild.result = 'FAILURE'
}

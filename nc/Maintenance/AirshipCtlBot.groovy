	import groovy.json.JsonSlurperClassic
	import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
	import hudson.triggers.TimerTrigger
	import hudson.model.Cause.UserIdCause

	run_airship_bot = {
		SCRIPT_PATH = "sudo -i -u ubuntu /home/ubuntu/bot/gerrit-to-github-bot/run_airshipctl_bot.sh"
		println("Run Airshipctl BOT. Fetch gerrit changes from last $SYNC_TIME_WINDOW")
		sh "$SCRIPT_PATH $SYNC_TIME_WINDOW"
	}

	//// main flow
	try {
		node (label: NODE_LABEL){
			cleanWs()
			run_airship_bot()
		}
	} catch (error) {
		print "Build failed: ${error.getMessage()}"
		currentBuild.result = 'FAILURE'
	}
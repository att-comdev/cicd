import groovy.json.JsonSlurperClassic
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import hudson.triggers.TimerTrigger
import hudson.model.Cause.UserIdCause

def run_on_slave = {
	try {
		cleanWs()
		print "COMMANDS to RUN on IDLE NODES FOR MAINTENANCE"
		sh "df -h"
		sh "free -m"

		sh "sudo ls -lrth /var/lib/libvirt/images/ | true"
		sh "sudo ls -lrth /tmp/ | true"

		print "Apt autoremove and Clean"
		sh "sudo apt-get autoremove -y --purge && sudo apt autoclean && sudo apt clean | true"

		print "Delete tmp dirs"
		sh "sudo rm -rf /tmp/*"
		sh "sudo rm -rf /var/tmp/*"

		print "Docker Cleanup"
		sh "sudo docker system prune -af | true"
		sh "sudo docker volume prune -f | true"

		print "Clean Virsh console logs"
		sh "sudo rm -rf /var/log/libvirt-consoles/air-worker-1-console.log"
		sh "sudo rm -rf /var/log/libvirt-consoles/air-target-1-console.log"
		sh "sudo rm -rf /var/log/libvirt-consoles/air-ephemeral-console.log"

		print " Clean journalctl logs "
		sh "sudo journalctl --vacuum-size=500M"

		print "clear apache old logs"
		sh "sudo rm -rf /var/log/apache2/*.gz"

	} catch(Exception error) {
		print "Build failed: ${error.getMessage()}"
	}
}

//// main flow
try {
	def slaves = []
	// Get the list of idle slaves
	hudson.model.Hudson.instance.slaves.each {
		if ( !it.getComputer().isOffline() && it.getComputer().countBusy() == 0 &&
			it.getLabelString() == WORKER_LABEL) {
				slaves << it.name
		}
	}

	println('*'*120)
	print "Cleanup scripts will be executed on these worker VMs"
	print(slaves)
	println('*'*120)

	slaves.each {
		print("Running on ${it} as it seems to be idle")
		node(it) {
		run_on_slave()
		}
	}
} catch (error) {
	print "Build failed: ${error.getMessage()}"
	currentBuild.result = 'FAILURE'
}
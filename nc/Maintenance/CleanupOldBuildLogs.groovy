    import groovy.json.JsonSlurperClassic
    import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
    import hudson.triggers.TimerTrigger
    import hudson.model.Cause.UserIdCause

    cleanup_log = {
        COMMAND = "sudo find /mnt/jenkins-data/userContent/ -mtime +${NO_OF_DAYS_TO_CLEANUP} -type f -delete"
        print "Command: ${COMMAND}"
        sh "sudo apt install -y sshpass"
        withCredentials([usernamePassword(credentialsId: "jenkins_master",
            usernameVariable: "USER",
            passwordVariable: "PASSWORD")]) {
                sh "sshpass -p ${PASSWORD} ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no  ${USER}@${HOST} ${COMMAND}"
        }
    }

    //// main flow
    try {
        node (label: NODE_LABEL){
            cleanWs()
            cleanup_log()
        }
    } catch (error) {
        print "Build failed: ${error.getMessage()}"
        currentBuild.result = 'FAILURE'
    }
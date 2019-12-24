path = "cicd/JumpHost"

pipelineJob(path) {
    description("This job creates a jumphost to access jenkins agents run in Openstack VMs")
    logRotator {
        daysToKeep(30)
    }
    definition {
        cps {
            script(readFileFromWorkspace("${path}/Jenkinsfile.groovy"))
            sandbox(false)
        }
    }
}

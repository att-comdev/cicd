base_path = "cicd"
job_path = "${base_path}/NodeCleanup"
folder("${base_path}")

pipelineJob(job_path) {
    logRotator{
        daysToKeep(180)
    }
    description("This job deletes the jenkins node and its underlying stack")
    logRotator {
        daysToKeep(180)
    }
    parameters {
        stringParam {
            name ('DELETE_NODENAME')
            defaultValue('')
            description('Node to be deleted')
        }
    }
    definition {
        cps {
            script(readFileFromWorkspace("${job_path}/Jenkinsfile"))
            sandbox()
        }
    }
}

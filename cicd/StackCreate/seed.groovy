base_path = "cicd/StackCreate"
job_path = "${base_path}/StackCreate"
folder("${base_path}")

pipelineJob(job_path) {
    description("This job executes heta stack create for Image stack")
    label('master')
    parameters {
        stringParam {
            name ('CLOUD_IMAGE')
            defaultValue('https://artifacts-aic.atlantafoundry.com/artifactory/ubuntu-images/bionic/current/bionic-server-cloudimg-amd64.img')
            description('Image reference to be used to cretae image stack')
        }

        stringParam {
            name ('STACK_NAME')
            defaultValue('ubuntu-genesis-1604')
            description('Stack name to be used to cretae image stack')
        }
    }
    definition {
        cps {
            script(readFileFromWorkspace("${job_path}/Jenkinsfile"))
            sandbox()
        }
    }
}
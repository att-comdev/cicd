
JOB_FOLDER='dev'
JOB_NAME='dev-vms'

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Provide VMs to developers')
    description('More description here')

    logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
    }

    parameters {
        stringParam {
            name ('flavor')
            defaultValue('m1.medium')
            description('Launch a VM with needed resources')
        }
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }
}


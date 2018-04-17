
JOB_FOLDER='cicd'
JOB_NAME='dev-vms'

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    logRotator {
        numToKeep(20)
        artifactNumToKeep(20)
    }

    parameters {
        stringParam {
            name ('vmid')
            description('Unique name to identify VM to be created')
        }

        choiceParam('OPTION', ['ubuntu.m1.small.yaml',
                               'ubuntu.m1.medium.yaml',
                               'ubuntu.m1.large.yaml',
                               'ubuntu.m1.xlarge.yaml',
                               'ubuntu.m1.xxlarge.yaml'])
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }
}


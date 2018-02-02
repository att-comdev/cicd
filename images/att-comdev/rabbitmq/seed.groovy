JOB_FOLDER='images'
JOB_NAME='rabbitmq'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        stringParam {
            name ('VERSION')
            defaultValue('3.7/debian')
            description('Image version. Ex: 3.6/alpine. Check repo for more details.')
        }
    }
    scm {
        github('docker-library/rabbitmq', 'master')
    }
    triggers {
        scm 'H * * * *'
    }
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
            sandbox()
        }
    }
}


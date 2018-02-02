JOB_FOLDER='images'
JOB_NAME='genesis'

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        stringParam {
            name ('ipAddress')
            defaultValue('8.8.8.8')
            description('iDRAC IP address')
        }
    }
    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
        }
    }
}


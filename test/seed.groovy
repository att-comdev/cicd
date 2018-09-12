JOB_FOLDER='test'
JOB_NAME='HiddenParams'

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Testing hidden params')
    description('Testing hidden params')

    parameters {
        stringParam {
            name ('PARAM1')
            defaultValue('hidden param 1')
            description('hidden param 1')
        }
        password {
            name ('PARAM2')
            defaultValue('hidden param 2')
            description('hidden param 2')
        }
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }

}


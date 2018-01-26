JOB_FOLDER='images'
JOB_NAME='coredns'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        stringParam {
            name ('GO_VERSION')
            defaultValue('1.9.3')
            description('GoLang version')
        }
    }
    scm {
        github('coredns/coredns', 'master')
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


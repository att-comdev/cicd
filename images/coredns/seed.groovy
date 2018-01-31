JOB_FOLDER='images'
JOB_NAME='coredns'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
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


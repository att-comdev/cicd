JOB_FOLDER='images'
JOB_NAME='kubernetes'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        stringParam {
            name ('RELEASE_BRANCH')
            defaultValue('release-1.8')
            description('Kube release: release-1.9')
        }
    }
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
            sandbox()
        }
    }
}

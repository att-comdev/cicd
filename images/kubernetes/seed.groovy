JOB_FOLDER='images'
JOB_NAME='kubernetes'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        stringParam {
            name ('RELEASE_BRANCH')
            defaultValue('release-1.8')
            description('Kube release branch here')
        }
    }
    scm {
        github('kubernetes/kubernetes', 'release-1.8')
    }
    triggers {
        scm 'H/2 * * * *'
    }
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
            sandbox()
        }
    }
}

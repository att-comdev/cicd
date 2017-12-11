JOB_FOLDER='images'
JOB_NAME='kubernetes'
RELEASE_BRANCH='release-1.8'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        stringParam {
            name ('RELEASE_BRANCH')
            defaultValue(RELEASE_BRANCH)
            description('Kube release branch here')
        }
    }
    scm {
        github('kubernetes/kubernetes', RELEASE_BRANCH)
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

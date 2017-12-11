
JOB_FOLDER="images/kubernetes"
folder(JOB_FOLDER)

//add new release pipelines : branches here:
def jobs = ["kubernetes-1.6":"release-1.6",
            "kubernetes-1.8":"release-1.8",
            "kubernetes-1.9":"release-1.9",
            "kubernetes-master":"master"]

jobs.each { name, branch ->
    JOB_NAME=name
    RELEASE_BRANCH=branch
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
            scm 'H * * * *'
        }
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

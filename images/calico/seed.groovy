JOB_FOLDER="images/calico"

folder(JOB_FOLDER)

COMMUNITY_PROJECTS = ['cni': 'cni-plugin',
                      'kube-controllers': 'kube-controllers',
                      'node': 'calico']

COMMUNITY_PROJECTS.each { project, repo ->

    pipelineJob("${JOB_FOLDER}/${project}") {
        parameters {
            stringParam {
                defaultValue("${repo}")
                description('Paas job repo')
                name ('JOB_REPO')
            }
        }
        scm {
            github("projectcalico/${repo}", "master")
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

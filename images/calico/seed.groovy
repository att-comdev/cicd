JOB_BASE="images/calico"

folder(JOB_BASE)

PROJECTS = ['cni': 'cni-plugin',
            'kube-controllers': 'kube-controllers',
            'node': 'calico']

PROJECTS.each { project, repo ->

    pipelineJob("${JOB_BASE}/${project}") {
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
            cron('H */6 * * *')
        }
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_BASE}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

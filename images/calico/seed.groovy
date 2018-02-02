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
            git {
                remote {
                    name('ProjectCalico')
                    url("git@server:projectcalico/${repo}.git")
                }
                branch('master')
                extensions {
                    relativeTargetDirectory('calico')
                }
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
                script(readFileFromWorkspace("${JOB_BASE}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

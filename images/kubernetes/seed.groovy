
JOB_FOLDER="images/kubernetes"
folder(JOB_FOLDER)

import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()

//add new release branches here:
def jobsJson = '''{"jobs":[
                        {"name"  :"kubernetes-1.6",
                         "branch":"release-1.6"},
                        {"name"  :"kubernetes-1.8",
                         "branch":"release-1.8"},
                        {"name"  :"kubernetes-1.9",
                         "branch":"release-1.9"},
                        {"name"  :"kubernetes-master",
                         "branch":"master"}
                    ]}'''

def Json = jsonSlurper.parseText(jobsJson)

for (job in Json.jobs){
    RELEASE_BRANCH=job.release
    JOB_NAME=job.name
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

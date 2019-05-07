JOB_FOLDER="images/ubuntu-base-image"
folder(JOB_FOLDER)

def projects = ['ubuntu']

projects.each { project_name ->
    JOB_BASE_NAME=project_name
    pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
        description("This job builds the Ubuntu Base image")
        logRotator {
            daysToKeep(90)
        }
        triggers {
            definition {
                cps {
                    script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
                    sandbox(false)
                }
            }
        }
    }
}

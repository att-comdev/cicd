JOB_FOLDER="images"
folder(JOB_FOLDER)

JOB_BASE_NAME="code-review-image-build"
pipelineJob("${JOB_FOLDER}/${JOB_BASE_NAME}") {
    description("This job builds the image used in code-review pipeline")
    logRotator {
        daysToKeep(90)
    }
    parameters {
        stringParam {
            name ('BASE_IMAGE')
            defaultValue('')
            description('Base image defaults to com.att.nccicd.config.conf.POD_IMAGE_1604')
        }
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

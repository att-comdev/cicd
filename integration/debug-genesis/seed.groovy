JOB_FOLDER='integration'
JOB_NAME='debug-genesis'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    concurrentBuild(false)

    blockOn('genesis-full', 'site-update')
    triggers {
        buildResult('H */4 * * *') {
            combinedJobs()
            triggerInfo('genesis-full, site-update', BuildResult.SUCCESS, BuildResult.UNSTABLE, BuildResult.FAILURE)
        }
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
                sandbox(false)
            }
        }
    }
}

JOB_FOLDER='integration'
JOB_NAME='logs'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    concurrentBuild(false)

    properties {
        buildBlockerProperty {
            blockingJobs(genesis-full)
        }
    }
    triggers {
        upstream {
            upstreamProjects('genesis-full, site-update')
            threshold('FAILURE')
        }
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
                sandbox(false)
            }
        }
    }
}

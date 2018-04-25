JOB_FOLDER='integration'
JOB_NAME='logs'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    concurrentBuild(false)

    blockOn(['genesis-full', 'site-update']) {
        blockLevel('GLOBAL')
        scanQueueFor('ALL')
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

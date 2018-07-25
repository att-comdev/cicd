JOB_BASE = 'integration/osh-multi'
folder(JOB_BASE)

pipelineJob("${JOB_BASE}/osh-multi") {

    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '3'
        }
    }

    triggers {
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_BASE}/osh-multi/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

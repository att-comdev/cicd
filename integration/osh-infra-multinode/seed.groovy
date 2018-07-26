JOB_BASE = 'integration/osh-infra-multinode'
folder(JOB_BASE)

pipelineJob("${JOB_BASE}/osh-infra-multinode") {

    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '3'
        }
    }

    triggers {
        definition {
            cps {
                script(readFileFromWorkspace("${JOB_BASE}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

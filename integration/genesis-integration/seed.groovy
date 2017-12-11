base_path = "integration/genesis-integration"

pipelineJob("${base_path}/genesis-full") {

    parameters {
        stringParam {
            defaultValue('refs/changes/46/46/115')
            description('Pass att-comdev/cicd code refspec to the job')
            name ('CLCP_INTEGRATION_REFSPEC')
        }
        booleanParam {
            defaultValue(false)
            description('Enable Shipyard for Drydock and Armada operator')
            name ('SHIPYARD_ENABLED')
        }
        booleanParam {
            defaultValue(false)
            description('Enable Sonobuoy conformance tests')
            name ('SONOBUOY_ENABLED')
        }
    }

    concurrentBuild(false)

    triggers {
        cron('H */4 * * *')

        definition {
            cps {
                script(readFileFromWorkspace("${base_path}/Jenkinsfile"))
                sandbox()
            }
        }
    }
}


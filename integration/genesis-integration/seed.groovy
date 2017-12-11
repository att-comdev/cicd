base_path = "integration/genesis-integration"

pipelineJob("${base_path}/genesis-full") {

    parameters {
        stringParam {
            defaultValue(GERRIT_REFSPEC)
            description('Pass att-comdev/cicd code refspec to the job')
            name ('CICD_GERRIT_REFSPEC')
        }
        stringParam {
            defaultValue('refs/changes/46/46/114')
            description('Pass att-comdev/cicd code refspec to the job')
            name ('CLCP_INTEGRATION_REFSPEC')
        }
        booleanParam {
            defaultValue(true)
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


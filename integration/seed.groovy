base_path = "integration"

import groovy.json.JsonSlurper

def imagesJson = '''{ "genesis":[
                          "genesis-full",
                          "site-update"
                        ]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.genesis) {
    pipelineJob("${base_path}/${entry}") {
        parameters {
            stringParam {
                defaultValue('refs/changes/67/403067/1')
                description('Pass att-comdev/cicd code refspec to the job')
                name ('CLCP_INTEGRATION_REFSPEC')
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
                    script(readFileFromWorkspace("${base_path}/${entry}/Jenkinsfile"))
                    sandbox()
                }
            }
        }
    }
}

import groovy.json.JsonSlurper

folder("deployment_engineer")
pipelineJob("deployment_engineer/passphrase") {
    disabled(false)
    parameters {
             stringParam('MASTER_PASSPHRASE',"${entry.repo_name}")
             stringParam('PROJECT',"${entry.git_repo}")
             choiceParam('PEGLEG_COMMAND', ["encrypt_passphrase", "rotate_passphrase"])
    }
    triggers {

        definition {
            cps {
                script(readFileFromWorkspace('deployment_engineers/passphrase/Jenkinsfile'))
                sandbox()
            }
        }
    }
}

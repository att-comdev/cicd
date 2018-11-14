folder("integration")
folder("integration/uplift")
pipelineJob("integration/uplift/create-versions") {
    disabled(false)
    logRotator{
        daysToKeep(90)
    }
    parameters {
        stringParam('CICD_REFSPEC', "refs/changes/53/432353/15")
    }
    triggers {
        definition {
            cps {
                script(readFileFromWorkspace('integration/uplift/Jenkinsfile'))
                sandbox(false)
            }
        }
    }
}
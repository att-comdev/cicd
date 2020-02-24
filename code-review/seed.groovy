pipelineJob("code-review") {
    description("This job is for python applications to run pep8, bandit, unit tests, and code coverage")
    logRotator{
        daysToKeep(90)
    }
    parameters {
        stringParam {
            defaultValue("")
            description('Internal project name for manual build.')
            name ('PROJECT_NAME')
            trim(true)
        }
        stringParam {
            defaultValue("")
            description('Reference for manual build of internal project.\n\n' +
                        'Branch or gerrit refspec is supported.')
            name ('PROJECT_REF')
            trim(true)
        }
        stringParam {
            defaultValue("")
            description('Branch for manual build of internal project.\n\n')
            name ('PROJECT_BRANCH')
            trim(true)
        }
    }
    triggers {
        definition {
            cps {
                script(readFileFromWorkspace("code-review/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

PROJECT_FOLDER="cicd"
PROJECT_NAME="CommonFunctionsTest"

folder("${PROJECT_FOLDER}")
pipelineJob("${PROJECT_FOLDER}/${PROJECT_NAME}") {
    parameters {
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('refs/changes/57/389757/9')
            description('current refspec')
        }
    }
    parameters {
        stringParam {
            name ('TEST_PARAM1')
            defaultValue('Test1' )
            description('Test job parameter')
        }
    }
    parameters {
        stringParam {
            name ('TEST_PARAM2')
            defaultValue('Test2')
            description('Test job parameter')
        }
    }
    definition {
        cps {
            script(readFileFromWorkspace("${PROJECT_FOLDER}/${PROJECT_NAME}/Jenkinsfile"))
            sandbox()
        }
    }

}


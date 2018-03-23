JOB_FOLDER="packages"
JOB_NAME="BuildPackages"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Build Packages')
    description('This job is supposed to build (backport/re-build) custom Ubuntu packages.')

    logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }

}


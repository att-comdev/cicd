JOB_FOLDER="packages"
JOB_NAME="BuildPackages"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Build Packages')
    description('\nThis job is supposed to build (backport/re-build) custom Ubuntu packages and upload them to Artifactory.\nList of packages is in Jenkinsfile, submit a change to amend it.')

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


JOB_FOLDER="packages/ubuntu"
JOB_NAME="BuildPackages"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    logRotator{
        daysToKeep(180)
    }
    displayName('Build Packages')
    description('\nThis job is supposed to build (backport/re-build) custom '+
                'Ubuntu packages and upload them to Artifactory.\nList of '+
                'packages is in Jenkinsfile, submit a change to amend it.')
    parameters {
        choiceParam('UPLOAD_PACKAGES', ['false', 'true'], 'Upload packages to repository')
        choiceParam('FAKE_GPG_KEY', ['false', 'true'], 'Use fake GPG key')
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }

}

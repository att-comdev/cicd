JOB_FOLDER="images/att-comdev/ubuntu/cloud"
JOB_NAME="BuildCloudImages"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Build localized Ubuntu cloud images')
    description('\nThis job is supposed to build (re-build) custom Ubuntu '+
                'cloud images and upload them to Artifactory.\nList of '+
                'cloud images is in Jenkinsfile, submit a change to amend it.')

    logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
    }

    parameters {
        choiceParam('UPLOAD_IMAGES', ['false', 'true'], 'Upload cloud image to the repository')
        choiceParam('UBUNTU_RELEASE', ['xenial', 'bionic', 'artful'], 'Ubuntu release to build')
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }

}

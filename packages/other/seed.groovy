JOB_FOLDER="packages/other"
JOB_NAME="BuildPackages"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Build Generic Package')
    description('\nThis job is supposed to build custom generic packages for'+
                ' Ubuntu from Gerrit repo and upload it to Artifactory.\n\n'+
                ' Supports anonymous Gerrit clone/fetch access via HTTPS or'+
                ' SSH access with user name and SSH key stored as Jenkins'+
                ' credential.')

    logRotator {
        numToKeep(50)
        artifactNumToKeep(50)
    }

    parameters {
        stringParam {
            name ('GERRIT_HOST')
            defaultValue('review.gerrithub.io')
            description('Gerrit server HOSTNAME for anonymous access via '+
                        'HTTPS, or via SSH. Provide port for SSH access '+
                        '(most probably :29418).')
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('att-comdev/cicd')
            description('Gerrit project')
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('refs/changes/48/412048/1')
            description('Gerrit refspec (refs/changes/xx/xxxxx/x)')
        }
        credentialsParam('GERRIT_SSH_CREDS') {
            type('com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey')
            required(true)
            defaultValue('')
            description('SSH Key for deploying build artifacts')
        }
        choiceParam('UPLOAD_PACKAGES', ['false', 'true'],
                        'Upload packages to repository')
        stringParam {
            name ('ARTF_REPOSITORY_PATH')
            defaultValue('genesis/')
            description('Path on artifactory repository where packages '+
                        'would be uploaded')
        }
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }

}

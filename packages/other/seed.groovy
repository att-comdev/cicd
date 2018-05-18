JOB_FOLDER="packages/other"
JOB_NAME="BuildPackages"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Build Generic Package')
    description('\nThis job is supposed to build custom generic packages for'+
                ' Ubuntu from Gerrit repo and upload it to Artifactory.\n\n'+
                ' Supports anonymous Gerrit clone/fetch access via HTTPS or'+
                ' SSH access with user name and SSH key stored as Jenkins'+
                ' credential. Requires build.sh script.')

    parameters {
        stringParam {
            name ('GERRIT_HOST')
            defaultValue('review.gerrithub.io:29418')
            description('Gerrit server HOSTNAME[:port] for anonymous access '+
                        'via HTTPS, or via SSH. Provide port for SSH access '+
                        '(most probably :29418).')
            trim(true)
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('att-comdev/cicd')
            description('Gerrit project')
            trim(true)
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('refs/changes/48/412048/6')
            description('Gerrit refspec (refs/changes/xx/xxxxx/x)')
            trim(true)
        }
        credentialsParam('GERRIT_SSH_CREDS') {
            type('com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey')
            defaultValue('rg445d-tmp-gerrit')
            description('SSH credentials to access Gerrit (user name and '+
                        'private key) stored in Jenkins')
        }
        stringParam {
            name ('PACKAGE')
            defaultValue('package')
            description('Package name')
            trim(true)
        }
        stringParam {
            name ('PACKAGE_VERSION')
            defaultValue('v0.1')
            description('Package version')
            trim(true)
        }
        choiceParam('UPLOAD_PACKAGES', ['true', 'false'],
                        'Upload packages to repository')
        stringParam {
            name ('ARTF_REPOSITORY_PATH')
            defaultValue('genesis/')
            description('Path on artifactory repository where packages '+
                        'would be uploaded')
            trim(true)
        }
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }

}

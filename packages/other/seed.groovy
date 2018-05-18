JOB_FOLDER="packages/other"
JOB_NAME="BuildPackages"

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Build Generic Package')
    description('\nThis job is supposed to build custom generic '+
                'package for Ubuntu and upload it to Artifactory.')

    logRotator {
        numToKeep(50)
        artifactNumToKeep(50)
    }

    parameters {
        stringParam {
            name ('GERRIT_URL')
            defaultValue('https://')
            description('Gerrit URL')
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('')
            description('Gerrit project')
        }
        stringParam {
            name ('GERRIT_BRANCH')
            defaultValue('master')
            description('Gerrit branch')
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('')
            description('Gerrit refspec (refs/changes/xx/xxxxx/x)')
        }
        choiceParam('UPLOAD_PACKAGES', ['false', 'true'], 'Upload packages to repository')
        stringParam {
            name ('ARTF_REPOSITORY_PATH')
            defaultValue('')
            description('Path on artifactory repository where packages would be uploaded')
        }
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }

}

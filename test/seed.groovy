/**
 * Testing library functions
 */

JOB_FOLDER='test'
JOB_NAME='CCICD-279-LibraryTest'

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Testing library in vars/')
    description('This job is supposed to call methods from libraries located in vars/ to test if they work properly')

    logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
    }

    parameters {
        credentials {
            name ('creds')
            defaultValue('jenkins-artifactory')
            description('Artifactory credentials to use')
        }
        stringParam {
            name ('url')
            defaultValue('https://${ARTF_DOCKER_URL}/aic-clcp-manifests')
            description('URL of artifact onto which we would set properties')
        }
        stringParam {
            name ('prop')
            defaultValue('key1=value1;key2=value2')
            description('Properties we are setting onto the artifact')
        }
        stringParam {
            name ('image')
            defaultValue('${IMAGE}')
            description('Docker image to use when getting it\'s Digest')
        }
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
        }
    }

}


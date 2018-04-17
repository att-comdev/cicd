
JOB_FOLDER='cicd/dev-vms'
JOB_NAME='dev-vms'

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    logRotator {
        numToKeep(20)
        artifactNumToKeep(20)
    }

    parameters {

        stringParam {
            name ('NODE_POSTFIX')
            defaultValue('user-vm1')
            description('Unique name to identify heat stack to be created')
        }

        stringParam {
            name ('SSH_KEY')
            defaultValue('')
            description('Public SSH key to access the VM')
        }

        choiceParam('NODE_DATA', ['bootstrap.sh'],
                    'Select user data based on resources/heat/stack')
 
        choiceParam('NODE_IMAGE', ['cicd-ubuntu-16.04-server-cloudimg-amd64'],
                    'Select image for the VM')

        choiceParam('NODE_FLAVOR', ['m1.medium', 'm1.large', 'm1.xlarge'],
                    'Select flavor for the VM')
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }
}


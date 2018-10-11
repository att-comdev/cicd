
JOB_FOLDER='dev'
JOB_NAME='vms'

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    description("This job allows to create a VM on demand, and keep it running")

    logRotator {
        daysToKeep(30)
    }

    parameters {

        stringParam {
            name ('NODE_POSTFIX')
            defaultValue('user-vm1')
            description('Unique name to identify heat stack to be created')
            trim(true)
        }

        stringParam {
            name ('SSH_KEY')
            defaultValue('')
            description('Public SSH key to access the VM')
            trim(true)
        }

        choiceParam('NODE_DATA', ['bootstrap.sh'],
                    'Select user data based on resources/heat/stack')

        choiceParam('NODE_IMAGE', ['cicd-ubuntu-16.04-server-cloudimg-amd64',
                                   'cicd-ubuntu-18.04-server-cloudimg-amd64'],
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



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

        choiceParam('NODE_TMPL', ['ubuntu.m1.small.yaml',
                               'ubuntu.m1.medium.yaml',
                               'ubuntu.m1.large.yaml',
                               'ubuntu.m1.xlarge.yaml',
                               'ubuntu.m1.xxlarge.yaml'],
                    'Select template based on resources/heat')

        choiceParam('NODE_DATA', ['bootstrap.sh'],
                    'Select user data based on resources/heat')
 
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
        }
    }
}


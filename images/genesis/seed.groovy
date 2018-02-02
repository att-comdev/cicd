JOB_FOLDER='images'
JOB_NAME='genesis'

folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {

    displayName('Genesis builder')
    description('This job is supposed to build ISO and boot/deploy Genesis node')

    logRotator {
        numToKeep(5)
        artifactNumToKeep(5)
    }

    parameters {
        stringParam {
            name ('iDracIpAddress')
            defaultValue('')
            description('iDRAC IP address')
        }
        stringParam {
            name ('iDracUser')
            defaultValue('')
            description('iDRAC user')
        }
        password {
            name ('iDracPassword')
            defaultValue('')
            description('iDRAC user password')
        }
        stringParam {
            name ('preseedUrl')
            defaultValue('')
            description('Location of preseed.cfg, http(s) URL')
        }
        stringParam {
            name ('bootIso')
            defaultValue('')
            description('Location of ISO to boot server from')
        }
    }

    definition {
        cps {
            sandbox()
            script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
        }
    }

}


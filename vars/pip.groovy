/**
 * Update hosts and install python and pip
 */
def updateHost() {
    sh "sudo apt-get update"
    sh "sudo apt install python-minimal -y"
    sh "sudo apt install python-pip -y"
}

/**
 * Build pip packages for corresponding OpenStack client and upload to Artifactory repo

 * @param gerritProject gerrit project for OpenStack client
 */
def buildPackageAndUpload(String gerritProject) {
    dir (gerritProject) {
        sh "python setup.py sdist bdist_wheel upload -r local"
    }

}

/**
 * Create pypirc file which can be used to upload packages to artifactory repo

 * @param credentialsId jenkins SSH credentials ID
 */
def createPypirc(String credentialsId) {

    withCredentials([usernamePassword(credentialsId: credentialsId,
                    usernameVariable: 'ARTIFACTORY_USER',
                    passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
        sh '''#!/bin/bash
cat << EOF > ~/.pypirc
[distutils]
index-servers = local
[local]
repository: ${ARTF_LOCAL_PYPI_URL}
username: ${ARTIFACTORY_USER}
password: ${ARTIFACTORY_PASSWORD}
EOF
'''
    }
}
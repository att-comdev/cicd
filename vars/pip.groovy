/**
 * Update hosts and install python and pip
 */
def updateHost() {
    sh "sudo apt-get update"
    sh "sudo apt install python-minimal -y"
    sh "sudo apt install python-pip -y"
}

/**
 * Build pip packages and upload to Artifactory repo

 * @param project project dir to build
 */
def buildPackageAndUpload(String project) {
    dir (project) {
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

/**
 * Install pip packages
 * @param packages list of packages to install
 */
def installPackages(List packages) {
    sh "pip install ${packages.join(' ')}"
}

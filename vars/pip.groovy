def update_host() {
    sh "sudo apt-get update"
    sh "sudo apt install python-minimal -y"
    sh "sudo apt install python-pip -y"
}

def build_package_and_upload(String gerrit_project) {
    dir ("${gerrit_project}") {
        sh "python setup.py sdist bdist_wheel upload -r local"
    }

}

def create_pypirc(String credentials_id) {

    withCredentials([usernamePassword(credentialsId: credentials_id,
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
package org.slave

def gitClone() {
    git 'https://review.gerrithub.io/att-comdev/cicd'
}

def getHostname() {
    sh 'hostname'
}


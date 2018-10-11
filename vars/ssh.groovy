
def sshCmd = { String creds, String ip, String cmd ->

    withCredentials([sshUserPrivateKey(credentialsId: creds,
            keyFileVariable: 'SSH_KEY',
            usernameVariable: 'SSH_USER')]) {

        sh "ssh -i ${SSH_KEY} ${SSH_USER}@${ip} ${cmd}"
    }
}

def sshWait (String creds, String ip ) {
    retry (12) {
        try {
            sshCmd (creds, ip, '-o StrictHostKeyChecking=no hostname')
        } catch (err) {
            sleep 60
            error(err)
        }
    }
}

def scpPut = { String creds, String ip, String src, String dst ->

    withCredentials([sshUserPrivateKey(credentialsId: creds,
            keyFileVariable: 'SSH_KEY',
            usernameVariable: 'SSH_USER')]) {

        sh "scp -i ${SSH_KEY} ${src} ${SSH_USER}@${ip}:${dst}"
    }
}

def scpGet = { String creds, String ip, String src, String dst ->

    withCredentials([sshUserPrivateKey(credentialsId: creds,
            keyFileVariable: 'SSH_KEY',
            usernameVariable: 'SSH_USER')]) {

        sh "scp -i ${SSH_KEY} ${SSH_USER}@${ip}:${src} ${dst}"
    }
}


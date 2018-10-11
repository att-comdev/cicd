
def cmd = { String creds, String ip, String cmd ->

    withCredentials([sshUserPrivateKey(credentialsId: creds,
            keyFileVariable: 'SSH_KEY',
            usernameVariable: 'SSH_USER')]) {

        sh "ssh -i ${SSH_KEY} ${SSH_USER}@${ip} ${cmd}"
    }
}

def wait (String creds, String ip ) {
    retry (12) {
        try {
            cmd (creds, ip, '-o StrictHostKeyChecking=no hostname')
        } catch (err) {
            sleep 60
            error(err)
        }
    }
}

def put = { String creds, String ip, String src, String dst ->

    withCredentials([sshUserPrivateKey(credentialsId: creds,
            keyFileVariable: 'SSH_KEY',
            usernameVariable: 'SSH_USER')]) {

        sh "scp -i ${SSH_KEY} ${src} ${SSH_USER}@${ip}:${dst}"
    }
}

def get = { String creds, String ip, String src, String dst ->

    withCredentials([sshUserPrivateKey(credentialsId: creds,
            keyFileVariable: 'SSH_KEY',
            usernameVariable: 'SSH_USER')]) {

        sh "scp -i ${SSH_KEY} ${SSH_USER}@${ip}:${src} ${dst}"
    }
}


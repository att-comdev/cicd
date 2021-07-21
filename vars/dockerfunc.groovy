def login(String docker_repo,String creds) {
    withCredentials([usernamePassword(credentialsId: "${creds}",
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASSWORD')]) {
       opts = '-u $DOCKER_USER -p $DOCKER_PASSWORD'
       sh """
         docker login ${opts} ${docker_repo}
       """
    }
}

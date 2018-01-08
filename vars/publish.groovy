
//Format for method names
//{artifact_repo}_{what is being published}
//For example nexus_jenkins_log


//This will curl the Jenkins console logs and upload them to Nexus.
def nexus_jenkins_log (String org, String project, String repositoryName) {
// Usage example: publish.nexus_logs('openstack','openstack-helm', 'att-comdev-jenkins-logs')
    sh "curl -s -o ./${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}.log ${BUILD_URL}consoleText"
    nexusArtifactUploader artifacts: [[ artifactId: project,
                                        classifier: '',
                                        file: GERRIT_CHANGE_NUMBER+'-'+GERRIT_PATCHSET_NUMBER+'.log']],
                                        credentialsId: 'nexus3',
                                        groupId: org,
                                        nexusUrl: '$NEXUS3_URL',
                                        nexusVersion: 'nexus3',
                                        protocol: 'http',
                                        repository: repositoryName,
                                        version: '$BUILD_NUMBER'
}

//This will publish images to respository in repositoryID
def image (String repositoryID, String repositoryUrl, String imageName) {
  // Usage example: publish.image('jenkins-artifactory',"${ARTF_URL}",${ARMADA_IMAGE}")
  // Usage example: publish.image('jenkins-quay',"${QUAY_URL}",${QUAY_IMAGE}")
   withCredentials([usernamePassword(credentialsId: repositoryID,
                    usernameVariable: 'REPO_USER',
                    passwordVariable: 'REPO_PASSWORD')]) {

                    opts = '-u $REPO_USER -p $REPO_PASSWORD'
                    sh "sudo docker login ${opts} ${repositoryUrl}"
                    sh "sudo docker push ${imageName}"
   }
}

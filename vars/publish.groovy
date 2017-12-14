
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

//This will publish images to Artifactory
def artifactory_image (String artifactoryID, String artifactoryUrl, String imageName) {
  // Usage example: publish.artifactory_image('jenkins-artifactory',"${ARTF_URL}",${ARMADA_IMAGE}")
   withCredentials([usernamePassword(credentialsId: artifactoryID,
                    usernameVariable: 'ARTIFACTORY_USER',
                    passwordVariable: 'ARTIFACTORY_PASSWORD')]) {

                    opts = '-u $ARTIFACTORY_USER -p $ARTIFACTORY_PASSWORD'
                    sh "sudo docker login ${opts} ${artifactoryUrl}"
                    sh "sudo docker push ${imageName}"
   }
}
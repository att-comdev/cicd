
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
def image (String creds, String url, String src, String dst) {
  // Usage example: publish.image('jenkins-artifactory',"${ARTF_URL}",${ARMADA_IMAGE}")
  // Usage example: publish.image('jenkins-quay',"${QUAY_URL}",${QUAY_IMAGE}")
   withCredentials([usernamePassword(credentialsId: creds,
                    usernameVariable: 'REPO_USER',
                    passwordVariable: 'REPO_PASSWORD')]) {

       opts = '-u $REPO_USER -p $REPO_PASSWORD'
       sh "sudo docker login ${opts} ${url}"

       sh "sudo docker tag ${src} ${dst}"
       sh "sudo docker push ${dst}"
   }
}

def artifactory (String src, String dst) {
    image('jenkins-artifactory', ARTF_DOCKER_URL, src,
          "${ARTF_DOCKER_URL}/${dst}")
}

def quay (String src, String dst) {
    image('jenkins-quay', QUAY_URL, src, "${QUAY_URL}/${dst}")
}

def secureImage (String creds, String url, String src, String dst) {
    image('secure-artifactory', ARTF_SECURE_DOCKER_URL, src, dst)
}

/**
 * Sets property to the artifact (file/directory/respository) in Artifactory
 *
 * @param properties "key1=value1;key2=value2" string
 * @param url artifact URL, e.g. "${ARTF_DOCKER_URL}/clcp-manifests"
 * @param creds jenkins credentials ID; 'jenkins-artifactory' or
 *        'secure-artifactory' at the moment
 */
def setProperty (String creds, String url, String properties) {
// Usage example:
//    def imageDigest=build.getImageDigest(IMAGE)
//    publish.setProperty ('jenkins-artifactory', \
//        'https://artifacts-aic.atlantafoundry.com/artifactory/api/storage/clcp-manifests', \
//        "${RELEASE_CURRENT_KEY}=${imageDigest}")
    withCredentials([usernamePassword(credentialsId: creds,
                    usernameVariable: 'REPO_USER',
                    passwordVariable: 'REPO_PASSWORD')]) {

       opts = '-u $REPO_USER:$REPO_PASSWORD -X PUT'
       sh "curl ${opts} ${url}?properties=${properties}"
   }
}
/**
 * Sets property to the artifact (file/directory/respository) in Artifactory
 *
 * @param properties [key1: 'value1', key2: 'value2'] map
 * @param url artifact URL, e.g. "${ARTF_DOCKER_URL}/clcp-manifests"
 * @param creds jenkins credentials ID; 'jenkins-artifactory' or
 *        'secure-artifactory' at the moment
 */
def setProperty (String creds, String url, Map properties) {
      def p = properties.collect { it }.join(';')
      setProperty(creds, url, p)
}

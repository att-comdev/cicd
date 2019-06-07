import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
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
def image (String creds, String url, String src, String dst, useSudo=true) {
  // Usage example: publish.image('jenkins-artifactory',"${ARTF_URL}",${ARMADA_IMAGE}")
  // Usage example: publish.image('jenkins-quay',"${QUAY_URL}",${QUAY_IMAGE}")
   withCredentials([usernamePassword(credentialsId: creds,
                    usernameVariable: 'REPO_USER',
                    passwordVariable: 'REPO_PASSWORD')]) {

       def sudo = ""
       if (useSudo) {
           sudo = "sudo"
       }
       opts = '-u $REPO_USER -p $REPO_PASSWORD'
       sh "${sudo} docker login ${opts} ${url}"

       sh "${sudo} docker tag ${src} ${dst}"
       sh "${sudo} docker push ${dst}"
   }
}

def artifactory (String src, String dst, useSudo=true) {
    image('jenkins-artifactory', ARTF_DOCKER_URL, src,
          "${ARTF_DOCKER_URL}/${dst}", useSudo)
}

def quay (String src, String dst, useSudo=true) {
    image('jenkins-quay', QUAY_URL, src, "${QUAY_URL}/${dst}", useSudo)
}

def secureImage (String creds, String url, String src, String dst, useSudo=true) {
    image('secure-artifactory', ARTF_SECURE_DOCKER_URL, src, dst, useSudo)
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
//    def imageDigest=image.getImageDigest(IMAGE)
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

/**
 * Publish files (html, logs, xml, etc) artifacts to Artifactory
 *
 * @param file File to upload to Artifactory
 * @param repo Repository to upload artifact to
 * @param flat boolean: true (default) - upload and drop files and directories hierarchy, false - upload and maintain hierarchy
**/
def putArtifacts (String file, String repo, Boolean flat = true) {
     artf = Artifactory.server 'artifactory'

     spec = """{"files": [{
                "pattern": "${file}",
                "target": "${repo}",
                "flat": "${flat}"
            }]}"""

     artf.publishBuildInfo(artf.upload(spec))
}


/**
 * Delete files (html, logs, xml, etc) artifacts from Artifactory
 *
 * @param creds jenkins credentials ID; 'jenkins-artifactory' or
 *        'secure-artifactory' at the moment
 * @param url artifactory URL, e.g. "https://my-artifactory.com/artifactory"
 * @param repo Repository to remove artifact from, e.g. "aqua-docs"
 * @param path Path to the folder to remove from Artifactory without a
          trailing /
 * @param name Name of the file to remove from the given path, if no name is
 *        given, all files in the given path will be removed
**/
def deleteArtifacts (String creds, String url, String repo, String path,
                     String name="") {
    if (name == ""){
        data = ["repo": repo, "path": path, "name": ["\$match": "*"]]
    } else {
        data = ["repo": repo, "path": path, "name": name]
        path = path + "/" + name
    }
    jsonData = new JsonOutput().toJson(data)
    reqBody = "items.find(" + jsonData + ")"
    resp = httpRequest(url: "${url}/api/search/aql",
                       authentication: creds,
                       contentType: "TEXT_PLAIN",
                       httpMode: "POST",
                       quiet: true,
                       requestBody: reqBody)
    jsonResp = new JsonSlurperClassic().parseText(resp.content)
    total = jsonResp.range.total
    print "Attempting to delete ${total} artifacts."
    resp = httpRequest(url: "${url}/${repo}/${path}",
                       authentication: creds,
                       httpMode: "DELETE",
                       quiet: true,
                       validResponseCodes: '200:404')
    status = resp.status
    if (status >= 400){
        print "Deleting artifacts failed with API status code ${status}"
        return
    }
    msg = "The API status code ${status} was received from artifactory"
    print msg + " after the delete request."
    resp = httpRequest(url: "${url}/api/search/aql",
                       authentication: creds,
                       contentType: "TEXT_PLAIN",
                       httpMode: "POST",
                       quiet: true,
                       requestBody: reqBody)
    jsonResp = new JsonSlurperClassic().parseText(resp.content)
    total = jsonResp.range.total
    msg = "A total of ${total} artifacts currently match the given path"
    print msg + " " + path + " after the delete request."
}

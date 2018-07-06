import groovy.json.JsonOutput

/**
 * Creation of "configdocs" against a site's Deckhand,
 * utilizing the Shipyard CLI, that will be used to deploy
 * the rest of the site. This function should only be called
 * within a Shipyard CLI container.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param bucketName The name given to the collection of documents you're creating - typically matches the site name.
 */
def createConfigdocsWithinContainer(uuid, bucketName) {
    sh """
        shipyard --context-marker=${uuid} create configdocs ${bucketName} --replace --directory=${bucketName} > response
        cat response
    """

    def errors = getShipyardErrorCount() as Integer
    println "Shipyard Errors: " + errors
    if(errors.compareTo(0) == 1) {
        error("Build failed. Error(s) during Shipyard processing.")
    }
}

/**
 * Creation of "configdocs" against a site's Deckhand,
 * utilizing the Shipyard API, that will be used to deploy the
 * rest of the site.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param token An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param filePath A path to the file that you're looking to add to this collection of documents.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param bucketName The name given to the collection of documents you're creating - typically matches the site name.
 * @param bufferMode Indicates how the existing Shipyard Buffer should be handled - see: https://shipyard.readthedocs.io/en/latest/API.html?highlight=bufferMode for further details.
 */
def createConfigdocs(uuid, token, filePath, shipyardUrl, bucketName, bufferMode) {
    def res = httpRequest (url: shipyardUrl + "/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode}",
                          httpMode: "POST",
                          customHeaders: [[name: "Content-Type", value: "application/x-yaml"],
                                          [name: "X-Auth-Token", value: token],
                                          [name: "X-Context-Marker", value: uuid]],
                          quiet: true,
                          requestBody: filePath)
    return res
}

/**
 * Commitment of "configdocs" against a site's Deckhand,
 * utilizing the Shipyard CLI, that will be used to deploy
 * the rest of the site. This function should only be called
 * within a Shipyard CLI container.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 */
def commitConfigDocsWithinContainer(uuid) {
    sh """
        shipyard --context-marker=${uuid} commit configdocs > response
        cat response
    """
    def errors = getShipyardErrorCount() as Integer
    println "Shipyard Errors: " + errors
    if(errors.compareTo(0) == 1) {
        error("Build failed. Error(s) during Shipyard processing.")
    }
}

/**
 * Commitment of "configdocs" against a site's Deckhand,
 * utilizing the Shipyard API, that will be used to deploy
 * the rest of the site.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param token An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param shipyarUrl The Shipyard URL of the site you are creating documents against.
 */
def commitConfigdocs(uuid, token, shipyardUrl) {
    def res = httpRequest(url: shipyardUrl + "/api/v1.0/commitconfigdocs",
                          httpMode: "POST",
                          customHeaders: [[name: "X-Auth-Token", value: token],
                                          [name: "X-Context-Marker", value: uuid]],
                          quiet: true)
    return res
}

/**
 * The creation of a Shipyard action, utilizing the CLI,
 * against a site. This function should only be called
 * within a Shipyard CLI container.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param action The action to perform - see: https://shipyard.readthedocs.io/en/latest/API_action_commands.html?highlight=action for further details.
 */
def createActionWithinContainer(uuid, action) {
    return sh(script: "shipyard --context-marker=${uuid} create action ${action}", returnStdout: true).trim()
}

/**
 * The creation of a Shipyard, utilizing its API,
 * against a site.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param token An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param shipyardUrl The Shipyard FQDN of the site you are creating documents against.
 * @param action The action to perform - see: https://shipyard.readthedocs.io/en/latest/API_action_commands.html?highlight=action for further details.
 */
def createAction(uuid, token, shipyardUrl, action) {

    def req = ["name": action]
    def jreq = new JsonOutput().toJson(req)

    def res = httpRequest(url: shipyardUrl + "/api/v1.0/actions",
                          httpMode: "POST",
                          customHeaders: [[name: "Content-Type", value: "application/json"],
                                          [name: "X-Auth-Token", value: token],
                                          [name: "X-Context-Marker", value: uuid]],
                          quiet: true,
                          requestBody: jreq)
    return res
}

/**
 * Helper method to read the Shipyard CLI written
 * response to see if any errors were returned. This
 * is only necessary at this time because Shipyard
 * does not return a non-zero exit code when errors
 * are returned.
 *
 * @return String the number of errors returned by the Shipyard CLI request
 */
private def getShipyardErrorCount() {
    return sh(script: "tail -1 response | cut -d ':' -f2 | xargs | cut -d ',' -f1", returnStdout: true)
}

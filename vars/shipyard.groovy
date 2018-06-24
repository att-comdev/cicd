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
    def res = httpRequest(url: shipyardUrl + "/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode}",
                            httpMode: "POST'",
                            customHeaders: [[name: "Content-Type", value: "application/x-yaml"],
                                            [name: "X-Auth-Token", value: token],
                                            [name: "X-Context-Marker", value: uuid]],
                            requestBody: manifests)
    print res.content

    if(res.status != 201) {
        error("Failed to upload configdocs: ${res.status}")
    }
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
                                            [name: "X-Context-Marker", value: uuid]])
    print res.content

    if (res.status != 200) {
        error("Failed to commit configdocs: ${res.status}")
    }
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
 * @param authToken An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param shipyardFqdn The Shipyard FQDN of the site you are creating documents against.
 * @param action The action to perform - see: https://shipyard.readthedocs.io/en/latest/API_action_commands.html?highlight=action for further details.
 */
def createAction(uuid, authToken, shipyardFqdn, action) {
    def conn = new URL("${shipyardFqdn}/api/v1.0/actions").openConnection()
    conn.setRequestMethod("POST")
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("X-Context-Marker", uuid)
    conn.setRequestProperty("X-Auth-Token", authToken)
    conn.doOutput = true

    def requestJson = """ {
        "name": "${action}"
    } """

    conn.getOutputStream().write(requestJson.getBytes("UTF-8"))

    println "HTTP Response: " + conn.responseMessage
    def code = conn.responseCode as Integer
    println "HTTP Response Code: " + code
    if(code.compareTo(201) == 1) {
        error("Build failed. Bad HTTP Response Code.")
    }
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

/**
 * Reads the response code written from the Shipyard
 * API call.
 *
 * return String the response code returned by the Shipyard API request
 */
private def getResponseCode() {
    return sh(script: "cat response | grep 'HTTP/1.1' | tail -1 | grep -o '[0-9]\\{3\\}'", returnStdout: true)
}
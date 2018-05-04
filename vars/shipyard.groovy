def createConfigdocsWithinContainer(uuid, bucketName, outDir) {
    sh """
        shipyard --context-marker=${uuid} create configdocs ${bucketName} --replace --directory=${outDir} > response
        cat response
    """

    def errors = getShipyardErrorCount() as Integer
    println "Shipyard Errors: " + errors
    if(errors.compareTo(0) == 1) {
        error("Build failed. Error(s) during Shipyard processing.")
    }
}

def createConfigdocs(uuid, authToken, filePath, shipyardFqdn, bucketName, bufferMode) {
    sh """
        curl -iv -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/x-yaml' \
            --data-binary \"@${filePath}\" $shipyardFqdn/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode} > response
        cat response
    """

    def code = getResponseCode() as Integer
    println "HTTP Response Code: " + code
    if(code.compareTo(201) == 1) {
        error("Build failed. Bad HTTP Response Code.")
    }
}

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

def commitConfigdocs(uuid, authToken, shipyardFqdn) {
    sh """
        curl -iv -X POST -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' $shipyardFqdn/api/v1.0/commitconfigdocs > response
        cat response
    """

    def code = getResponseCode() as Integer
    println "HTTP Response Code: " + code
    if(code.compareTo(201) == 1) {
        error("Build failed. Bad HTTP Response Code.")
    }
}

def createActionWithinContainer(uuid, action) {
    return sh(script: "shipyard --context-marker=${uuid} create action ${action}", returnStdout: true).trim()
}

def createAction(uuid, authToken, shipyardFqdn, action) {
    def conn = new URL("${shipyardFqdn}/api/v1.0/actions").openConnection()
    conn.setRequestMethod("POST")
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("X-Context-Marker", "${uuid}")
    conn.setRequestProperty("X-Auth-Token", "${authToken}")
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

// This is currently necessary, as Shipyard doesn't return a non-zero exit code when errors are returned
private def getShipyardErrorCount() {
    return sh(script: "tail -1 response | cut -d ':' -f2 | xargs | cut -d ',' -f1", returnStdout: true)
}

private def getResponseCode() {
    return sh(script: "cat response | grep 'HTTP/1.1' | tail -1 | grep -o '[0-9]\\{3\\}'", returnStdout: true)
}
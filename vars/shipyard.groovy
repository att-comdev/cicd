def createConfigdocsWithinContainer(uuid, bucketName, outDir) {
    sh """
        shipyard --context-marker=${uuid} create configdocs ${SITE} --replace --directory=${OUTDIR} > response
        cat response
    """
    return getShipyardErrorCount()
}

def createConfigdocs(uuid, authToken, filePath, shipyardFqdn, bucketName, bufferMode) {
    sh "curl -iv -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/x-yaml' \
        --data-binary \"@${filePath}\" $shipyardFqdn/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode} > response"
    return getResponseCode()
}

def commitConfigDocsWithinContainer(uuid) {
    sh """
        shipyard --context-marker=${uuid} commit configdocs > response
        cat response
    """
    return getShipyardErrorCount()
}

def commitConfigdocs(uuid, authToken, shipyardFqdn) {
    def conn = new URL("${shipyardFqdn}/api/v1.0/commitconfigdocs").openConnection()
    conn.setRequestMethod("POST")
    conn.setRequestProperty("X-Context-Marker", "${uuid}")
    conn.setRequestProperty("X-Auth-Token", "${authToken}")
    conn.connect()

    println conn.responseMessage
    return conn.responseCode
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

    println conn.responseMessage
    return conn.responseCode
}

private def getShipyardErrorCount() {
    return sh(script: "tail -1 response | cut -d ':' -f2 | xargs | cut -d ',' -f1", returnStdOut: true).trim()
}

private def getResponseCode() {
    return sh(script: "cat response | grep 'HTTP/1.1' | tail -1 | grep -o '[0-9]\\{3\\}'", returnStdout: true).trim()
}
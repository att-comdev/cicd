def createConfigdocsWithinContainer(uuid, bucketName, outDir) {
    return sh(script: "shipyard --context-marker=${uuid} create configdocs ${bucketName} --replace --directory=${outDir}", returnStdout: true).trim()
}

def createConfigdocs(uuid, authToken, filePath, shipyardFqdn, bucketName, bufferMode) {
    sh "curl -iv -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/x-yaml' \
        --data-binary \"@${filePath}\" $shipyardFqdn/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode} > response"
    return getResponseCode()
}

def commitConfigDocsWithinContainer(uuid) {
    return sh(script: "shipyard --context-marker=${uuid} commit configdocs", returnStdout: true).trim()
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

private def getResponseCode() {
    return sh(script: "cat response | grep 'HTTP/1.1' | tail -1 | grep -o '[0-9]\\{3\\}'", returnStdout: true).trim()
}
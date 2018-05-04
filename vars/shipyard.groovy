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

def createConfigdocs(String uuid, String authToken, String filePath, String shipyardFqdn, String bucketName, String bufferMode) {
    sh "curl -iv -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/x-yaml' \
        --data-binary \"@${filePath}\" $shipyardFqdn/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode} > response"
    return getResponseCode()
}

def commitConfigdocs(String uuid, String authToken, String shipyardFqdn) {
    def conn = new URL("${shipyardFqdn}/api/v1.0/commitconfigdocs").openConnection()
    conn.setRequestMethod("POST")
    conn.setRequestProperty("X-Context-Marker", "${uuid}")
    conn.setRequestProperty("X-Auth-Token", "${authToken}")
    conn.connect()

    println conn.responseMessage
    return conn.responseCode
}

private def getResponseCode() {
    return sh(script: "cat response | grep 'HTTP/1.1' | tail -1 | grep -o '[0-9]\\{3\\}'", returnStdout: true).trim()
}
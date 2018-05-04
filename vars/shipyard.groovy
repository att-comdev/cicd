def createAction(uuid, authToken, shipyardFqdn, action) {

    def url = new URL("${shipyardFqdn}/api/v1.0/actions")
    def conn = url.openConnection()

    conn.setRequestMethod("POST")
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("X-Context-Marker", "${uuid}")
    conn.setRequestProperty("X-Auth-Token", "${authToken}")
    conn.doOutput = true

    def requestJson = """ {
        "name": "${action}"
    } """

    def writer = new OutputStreamWriter(conn.outputStream)
    writer.write(requestJson)
    writer.flush()
    writer.close()
    conn.connect()

    def responseCode = conn.responseCode
    return responseCode

}

def create_configdocs(String uuid, String authToken, String filePath, String shipyardFqdn, String bucketName, String bufferMode) {
    sh "curl -iv -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/x-yaml' \
        --data-binary \"@${filePath}\" $shipyardFqdn/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode} > response"
    return get_response_code()
}

def commit_configdocs(String uuid, String authToken, String shipyardFqdn) {
    sh "curl -iv -X POST -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/x-yaml' \
        $shipyardFqdn/api/v1.0/commitconfigdocs > response"
    return get_response_code()
}

private def get_response_code() {
    return sh(script: "cat response | grep 'HTTP/1.1' | tail -1 | grep -o '[0-9]\\{3\\}'", returnStdout: true).trim()
}
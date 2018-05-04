def createAction(uuid, authToken, shipyardFqdn, action) {

    def url = new URL("${shipyardFqdn}/api/v1.0/actions").openConnection()
    conn.setRequestMethod("POST")
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("X-Context-Marker", "${uuid}")
    conn.setRequestProperty("X-Auth-Token", "${authToken}")
    conn.doOutput = true

    def requestJson = """ {
        "name": "${action}"
    } """

    conn.getOutputStream().write(requestJson.getBytes("UTF-8"))
    return conn.responseCode
}

def create_configdocs(String uuid, String authToken, String filePath, String shipyardFqdn, String bucketName, String bufferMode) {
    sh "curl -iv -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/x-yaml' \
        --data-binary \"@${filePath}\" $shipyardFqdn/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode} > response"
    return get_response_code()
}

def createConfigdocs(String uuid, String authToken, String filePath, String shipyardFqdn, String bucketName, String bufferMode) {
        def conn = new URL("$shipyardFqdn/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode}").openConnection()
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Content-Type", "application/x-yaml")
        conn.setRequestProperty("X-Context-Marker", "${uuid}")
        conn.setRequestProperty("X-Auth-Token", "${authToken}")
        conn.doOutput = true

        conn.getOutputStream().write(new File("${filePath}").bytes)
        return conn.getHeaderField("X-Subject-Token")
}

def commit_configdocs(String uuid, String authToken, String shipyardFqdn) {
    sh "curl -iv -X POST -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/x-yaml' \
        $shipyardFqdn/api/v1.0/commitconfigdocs > response"
    return get_response_code()
}

private def get_response_code() {
    return sh(script: "cat response | grep 'HTTP/1.1' | tail -1 | grep -o '[0-9]\\{3\\}'", returnStdout: true).trim()
}
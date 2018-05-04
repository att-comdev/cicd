def create_action(String uuid, String authToken, String shipyardFqdn, String action) {
    sh "curl -iv -X POST -H 'X-Context-Marker: ${uuid}' -H 'X-Auth-Token: ${authToken}' -H 'Content-Type: application/json' \
        -d '{\"name\":\"${action}\"}' $shipyardFqdn/api/v1.0/actions > response"
    return get_response_code()
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
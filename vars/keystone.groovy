def retrieveToken(iamCredId, iamFqdn) {
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${iamCredId}",
            usernameVariable: 'user', passwordVariable: 'pw']]) {

        def url = new URL("${iamFqdn}/v3/auth/tokens")
        def conn = url.openConnection()

        conn.setRequestMethod("GET")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        def requestJson = """ {
            "auth": {
                "identity": {
                    "methods": ["password"],
                    "password": {
                        "user": {
                            "name": "${user}",
                            "domain": { "id": "default" },
                            "password": "${pw}"
                        }
                    }
                }
            }
        } """

        def writer = new OutputStreamWriter(conn.outputStream)
        writer.write(requestJson)
        writer.flush()
        writer.close()
        conn.connect()

        def result = conn.getHeaderField("X-Subject-Token")
        return result
    }
}
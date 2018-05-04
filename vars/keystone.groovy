def retrieveToken(iamCredId, iamFqdn) {
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${iamCredId}",
            usernameVariable: 'user', passwordVariable: 'pw']]) {

        def conn = new URL("${iamFqdn}/v3/auth/tokens").openConnection()
        //def conn = url.openConnection()

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

        //def writer = new OutputStreamWriter(conn.outputStream)
        //writer.write(requestJson)
        //writer.flush()
        //writer.close()
        //conn.connect()

        conn.getOutputStream().write(requestJson)

        def result = conn.getHeaderField("X-Subject-Token")
        return result
    }
}

def retrieve_token(iamCredId, iamFqdn) {
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${iamCredId}",
            usernameVariable: 'user', passwordVariable: 'pw']]) {

        def requestJson = """ {
            "auth": {
                "identity": {
                    "methods": ["password"],
                    "password": {
                        "user": {
                            "name": "$user",
                            "domain": { "id": "default" },
                            "password": "$pw"
                        }
                    }
                }
            }
        } """

        println('RequestJson: '+requestJson)
        def response = httpRequest contentType: 'APPLICATION_JSON', httpMode: 'GET', requestBody: JsonOutput.toJson(requestJson), url: "${iamFqdn}/v3/auth/tokens"
        println('Status: '+response.status)
        println('Response: '+response.content)
        return response.status

    }
}
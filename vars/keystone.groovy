def retrieveToken(iamCredId, iamFqdn) {
    withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: iamCredId,
            usernameVariable: "user", passwordVariable: "pw"]]) {

        def conn = new URL("${iamFqdn}/v3/auth/tokens").openConnection()
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

        conn.getOutputStream().write(requestJson.getBytes("UTF-8"))
        return conn.getHeaderField("X-Subject-Token")
    }
}
import groovy.json.JsonOutput

/**
 * Retrieve a token from a site's IAM (Keystone
 * in the UCP namespace) that can potentially be
 * used to talk to and perform actions with the
 * other UCP components.
 *
 * @param iamCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone
 * @param iamFqdn The IAM FQDN of the site you are authenticating against.
 * @return String The token supplied by IAM upon successful authentication
 */
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

// Clean version of getting Keystone token - todo: get into shared lib
def getKeystoneToken(iamCredId, iamFqdn) {

    withCredentials([[$class: "UsernamePasswordMultiBinding",
                      credentialsId: iamCredId,
                      usernameVariable: "user",
                      passwordVariable: "pass"]]) {

        def req = ["auth": [
                   "identity": [
                     "methods": ["password"],
                     "password": [
                       "user": ["name": user,
                                "domain": ["id": "default"],
                                "password": pass]]]]]

        def jreq = new JsonOutput().toJson(req)

        def res = httpRequest(url: iamFqdn + "/v3/auth/tokens",
                               contentType: "APPLICATION_JSON",
                               httpMode: "POST",
                               requestBody: jreq)

        return res.getHeaders()["X-Subject-Token"][0]
    }
}
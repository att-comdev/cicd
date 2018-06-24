import groovy.json.JsonOutput

/**
 * Retrieve a token from a site's IAM (Keystone
 * in the UCP namespace) that can potentially be
 * used to talk to and perform actions with the
 * other UCP components.
 *
 * @param iamCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone
 * @param iamUrl The IAM URL of the site you are authenticating against.
 * @return String The token supplied by IAM upon successful authentication
 */
def retrieveToken(iamCredId, iamUrl) {

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

        def res = httpRequest(url: iamUrl + "/v3/auth/tokens",
                               contentType: "APPLICATION_JSON",
                               httpMode: "POST",
                               requestBody: jreq)
        print res.content

        return res.getHeaders()["X-Subject-Token"][0]
    }
}
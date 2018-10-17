import groovy.json.JsonOutput

/**
 * Retrieve a token from a site's Keystone that can potentially be used to talk to and perform actions with the
 * other k8s cluster components.
 *
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 *                           In case of disable flag will use keystoneCredId as a password and username as user for keystone request.
 *                           Default value is True.
 * @return res The response supplied by the Keystone upon successful authentication
 */
def retrieveToken(keystoneCredId, keystoneUrl, withCreds=true, username='shipyard') {
    def password = keystoneCredId
    if (withCreds) {
        withCredentials([[$class: "UsernamePasswordMultiBinding",
                          credentialsId: keystoneCredId,
                          usernameVariable: "user",
                          passwordVariable: "pass"]]) {
            username = user
            password = pass
        }
    }

    def req = ["auth": [
               "identity": [
                 "methods": ["password"],
                 "password": [
                   "user": ["name": username,
                            "domain": ["id": "default"],
                            "password": password]]]]]

    def jreq = new JsonOutput().toJson(req)

    def res
    retry (3) {
        try {

            res = httpRequest(url: keystoneUrl + "/v3/auth/tokens",
                                  contentType: "APPLICATION_JSON",
                                  httpMode: "POST",
                                  quiet: true,
                                  requestBody: jreq)

        } catch (err) {
            sleep 120
            error(err)
        }
    }
    return res
}

import groovy.json.JsonOutput

/**
 * Retrieve a token from a site's Keystone that can potentially be used to talk to and perform actions with the
 * other k8s cluster components.
 *
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @return res The response supplied by the Keystone upon successful authentication
 */
def retrieveToken(keystoneCredId, keystoneUrl) {
    withCredentials([[$class: "UsernamePasswordMultiBinding",
                      credentialsId: keystoneCredId,
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

        def res = httpRequest(url: keystoneUrl + "/v3/auth/tokens",
                              contentType: "APPLICATION_JSON",
                              httpMode: "POST",
                              quiet: true,
                              requestBody: jreq)

        return res
    }
}

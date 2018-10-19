import groovy.json.JsonOutput


/**
 * Retrieve Kystone token
 * Usage:
 *     keystone.token(keystoneUrl: 'http://path/to/keystone',
 *                    keystoneCreds: 'keystone-user-pass-creds')
 *
 * @param keystoneUrl
 * @param keystoneCreds
 * @param retryCount
 * @param retryTimeout
 */
def token(Map map) {

    if (!map.containsKey('keystoneUrl')) {
        error("Must provide Keystone URL 'keystoneUrl'")
    } else if (!map.containsKey('keystoneCreds')) {
        error("Must provide Keystone credentials 'keystoneCreds'")
    }

    def retryCount = map.retryCount ?: 3.toInteger()
    def retryTimeout = map.retryTimeout ?: 120.toInteger()


    withCredentials([[$class: "UsernamePasswordMultiBinding",
                      credentialsId: map.keystoneCreds,
                      usernameVariable: "USER",
                      passwordVariable: "PASS"]]) {
        map.keystoneUser = USER
        map.keystonePassword = PASS
    }

    def req = ["auth": [
               "identity": [
                 "methods": ["password"],
                 "password": [
                   "user": ["name": map.keystoneUser,
                            "domain": ["id": "default"],
                            "password": map.keystonePassword ]]]]]

    def jreq = new JsonOutput().toJson(req)

    retry (map.retryCount) {
        try {
            def res = httpRequest(url: map.keystoneUrl + "/v3/auth/tokens",
                                  contentType: "APPLICATION_JSON",
                                  httpMode: "POST",
                                  quiet: true,
                                  requestBody: jreq)

            if (res.status != 200) {
                error (res.content)
            }
            print "Keystone token request succeesful: ${res.status}"
            return res.getHeaders()["X-Subject-Token"][0]

        } catch (err) {
            print "Keystone token request failed: ${err}"
            sleep retryTimeout
            error(err)
        }
    }
}


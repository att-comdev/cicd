import groovy.json.JsonOutput


def token(Map map) {

    def retryCount = map.retryCount ?: 3
    def retryTimeout = map.retryTimeout ?: 120

    withCredentials([[$class: "UsernamePasswordMultiBinding",
                      credentialsId: map.keystoneCredId,
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
                            "password": map.keystonePassword]]]]]

    def jreq = new JsonOutput().toJson(req)

    retry (map.retryCount) {
        try {
            def res = httpRequest(url: keystoneUrl + "/v3/auth/tokens",
                                  contentType: "APPLICATION_JSON",
                                  httpMode: "POST",
                                  quiet: true,
                                  requestBody: jreq)

            return res.getHeaders()["X-Subject-Token"][0]

        } catch (err) {
            sleep map.retryTimeout
            error(err)
        }
    }
}


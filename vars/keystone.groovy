import groovy.json.JsonOutput

/**
 * Retrieve a token from a site's Keystone that can potentially be used to talk to and perform actions with the
 * other k8s cluster components.
 *
 * @param keystoneCreds Credentials (user+pass) established within Jenkins to authenticate against a site's Keystone.
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 *                           In case of disable flag will use keystoneCredId as a password and username as user for keystone request.
 *                           Default value is True.
 * @return res The response supplied by the Keystone upon successful authentication
 */
def retrieveToken(keystoneCreds, keystoneUrl, withCreds=true, username='shipyard') {

    if( !(keystoneCreds instanceof List)) {
        keystoneCreds = [keystoneCreds]
    }

    def res
    keystoneCreds.any {
        def password = it
        if (withCreds) {
            withCredentials([[$class: "UsernamePasswordMultiBinding",
                              credentialsId: it,
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

        res = httpRequest(url: keystoneUrl + "/v3/auth/tokens",
                          contentType: "APPLICATION_JSON",
                          httpMode: "POST",
                          quiet: true,
                          validResponseCodes: '200:504',
                          requestBody: jreq)

        if(res) {
            if(res.status == 201) {
                // this is like a for loop "break", get out of collection iterating
                print "Keystone token request succeesful: ${res.status}"
                true
            } else if(res.status == 401 && it != keystoneCreds.last()) {
                // this is like a for loop "continue", move to the next item in the collection
                print "Unauthorized exception. Check next creds."
                return
            // In case of keystone is not accessible or has some issues repeat with the same creds
            } else {
                retry(6) {
                    try {
                        res = httpRequest(url: keystoneUrl + "/v3/auth/tokens",
                                          contentType: "APPLICATION_JSON",
                                          httpMode: "POST",
                                          quiet: true,
                                          validResponseCodes: '200:504',
                                          requestBody: jreq)
                        if(res.status == 201) {
                            print "Keystone token request succeesful: ${res.status}"
                            return true
                        } else if(res.status == 401 && it != keystoneCreds.last()) {
                            // this is like a for loop "continue", move to the next item in the collection
                            print "Unauthorized exception. Check next creds."
                            return
                        error("Unexpected return code for token request ${res.status}.")
                    } catch(err) {
                        sleep 120
                        error(err.getMessage())
                    }
                }
            }
        }
    }
    return res
}

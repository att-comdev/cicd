import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic


/**
 * Retrieve Keystone token
 * Usage:
 *     keystone.token(keystoneUrl: 'http://keystone',
                      keystoneCreds: 'keystone-user-pass-creds')
 *     or
 *     keystone.token(keystoneUrl: 'http://keystone',
 *                    keystoneCreds: ['keystone-user-pass-creds', 'keystone-user-pass-creds2'])
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

    if( !(map.keystoneCreds instanceof List)) {
        map.keystoneCreds = [map.keystoneCreds]
    }

    // optional with defaults
    def retryCount = map.retryCount ?: 3.toInteger()
    def retryTimeout = map.retryTimeout ?: 120.toInteger()
    def res

    map.keystoneCreds.any {
        withCredentials([[$class: "UsernamePasswordMultiBinding",
                          credentialsId: it,
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


        res = httpRequest(url: map.keystoneUrl + "/v3/auth/tokens",
                              contentType: "APPLICATION_JSON",
                              httpMode: "POST",
                              quiet: true,
                              validResponseCodes: '200:503',
                              requestBody: jreq)

        if(res) {
            if(res.status == 201) {
                print "Keystone token request succeesful: ${res.status}"
                // this is like a for loop "break", get out of collection iterating
                true
            } else if(res.status == 401 && it != map.keystoneCreds.last()) {
                // this is like a for loop "continue", move to the next item in the collection
                return
            } else {
                retry(retryCount-1) {
                    try {
                        res = httpRequest(url: map.keystoneUrl + "/v3/auth/tokens",
                              contentType: "APPLICATION_JSON",
                              httpMode: "POST",
                              quiet: true,
                              requestBody: jreq)

                        print "Keystone token request succeesful: ${res.status}"
                        // this is like a for loop "break", get out of collection iterating
                        true
                    } catch(error) {
                        print "Keystone token request failed: ${error}"
                        sleep retryTimeout
                        throw error
                    }
                }
            }
        }
    }

    return res.getHeaders()["X-Subject-Token"][0]
}

/**
 * Check Keystone Token
 * Usage:
 *     keystone.token(keystoneUrl: 'http://keystone',
                      keystoneCreds: 'keystone-user-pass-creds')
 *     or
 *     keystone.token(keystoneUrl: 'http://keystone',
 *                    keystoneCreds: ['keystone-user-pass-creds', 'keystone-user-pass-creds2'])
 *
 * @param keystoneUrl
 * @param keystoneCreds
 * @param keystoneToken
 * @param retryCount
 * @param retryTimeout
 */
def checkToken(Map map) {

    if (!map.containsKey('keystoneToken')) {
        error("Must provide Keystone Token 'keystoneToken'")
    }

    def adminToken = token(keystoneUrl: map.keystoneUrl,
                           keystoneCreds: map.keystoneCreds)

    // optional with defaults
    def retryCount = map.retryCount ?: 3.toInteger()
    def retryTimeout = map.retryTimeout ?: 120.toInteger()
    def res

    retry(retryCount) {
        try {
            res = httpRequest(url: map.keystoneUrl + "/v3/auth/tokens",
                                httpMode: "GET",
                                customHeaders: [[name: "X-Auth-Token", value: adminToken],
                                                [name: "X-Subject-Token", value: map.keystoneToken]],
                                quiet: false)
        } catch(error) {
            print "Check Token Error: ${error}"
            sleep retryTimeout
            throw error
        }
    }

    return res
}

/**
 * Retrieve service id from Keystone for given service type
 * Usage:
 *     keystone.getServiceId(keystoneUrl: 'http://keystone',
 *                           token: 'token',
                             'serviceType': 'shipyard')
 *
 * @param keystoneUrl
 * @param token
 * @param serviceType Example: 'shipyard'
 * @param retryCount
 * @param retryTimeout
 */
def getServiceId(Map map) {

    if (!map.containsKey('keystoneUrl')) {
        error("Must provide Keystone URL 'keystoneUrl'")

    } else if (!map.containsKey('token')) {
        error("Must provide Keystone token 'token'")
    } else if (!map.containsKey('serviceType')) {
        error("Must provide 'serviceType'")
    }

    // optional with defaults
    def retryCount = map.retryCount ?: 3.toInteger()
    def retryTimeout = map.retryTimeout ?: 120.toInteger()
    retry (retryCount) {
        try {

            def res = httpRequest (url: map.keystoneUrl + "/v3/services?type=${map.serviceType}",
                                   httpMode: "GET",
                                   contentType: "APPLICATION_JSON",
                                   customHeaders: [[name: "X-Auth-Token", value: map.token]],
                                   quiet: true)
            services = new JsonSlurperClassic().parseText(res.content)
            service_id = services.services[0]["id"]
            return service_id

        } catch (err) {
            print "Failed to get ${map.serviceType} service id: ${err}"
            sleep retryTimeout
            error(err)
        }
    }
}


/**
 * Retrieve service endpoint from Keystone for given serviceId and serviceInterface.
 * Usage:
 *     keystone.getServiceEndpoint(keystoneUrl: 'http://keystone',
 *                                 token: 'token',
                                   serviceId': 'serviceId',
                                   serviceInterface: 'public')
 *
 * @param keystoneUrl
 * @param token
 * @param serviceId
 * @param serviceInterface Default values is 'public'
 * @param retryCount
 * @param retryTimeout
 */
def _getServiceEndpoint(Map map) {

    if (!map.containsKey('keystoneUrl')) {
        error("Must provide Keystone URL 'keystoneUrl'")

    } else if (!map.containsKey('token')) {
        error("Must provide Keystone token 'token'")
    } else if (!map.containsKey('serviceId')) {
        error("Must provide Keystone service id 'serviceId'")
    }

    // optional with defaults
    def retryCount = map.retryCount ?: 3.toInteger()
    def retryTimeout = map.retryTimeout ?: 120.toInteger()
    def serviceInterface = map.serviceInterface ?: "public"
    retry (retryCount) {
        try {

            def res = httpRequest (url: map.keystoneUrl + "/v3/endpoints?service_id=${map.serviceId}&interface=${serviceInterface}",
                                   httpMode: "GET",
                                   contentType: "APPLICATION_JSON",
                                   customHeaders: [[name: "X-Auth-Token", value: map.token]],
                                   quiet: true)
            endpoints = new JsonSlurperClassic().parseText(res.content)
            return endpoints.endpoints[0]["url"]

        } catch (err) {
            print "Failed to get endpoint for service ${serviceId}: ${err}"
            sleep retryTimeout
            error(err)
        }
    }
}

/**
 * Retrieve service endpoint from Keystone for given sevice type and interface.
 * Usage:
 *     keystone.getServiceEndpoint(keystoneUrl: 'http://keystone',
 *                                 token: 'token',
                                   serviceType': 'shipyard',
                                   serviceInterface: 'public')
 *
 * @param keystoneUrl
 * @param token
 * @param serviceType Example:'shipyard'
 * @param serviceInterface Default values is 'public'
 * @param retryCount
 * @param retryTimeout
 */
def getServiceEndpoint(Map map) {
    if (!map.containsKey('keystoneUrl')) {
        error("Must provide Keystone URL 'keystoneUrl'")

    } else if (!map.containsKey('token')) {
        error("Must provide Keystone token 'token'")
    } else if (!map.containsKey('serviceType')) {
        error("Must provide 'serviceType'")
    }
    def serviceInterface = map.serviceInterface ?: "public"

    service_id = getServiceId(keystoneUrl: map.keystoneUrl,
                              token: map.token,
                              serviceType: map.serviceType)
    endpoint = _getServiceEndpoint(keystoneUrl: map.keystoneUrl,
                              token: map.token,
                              serviceId: service_id,
                              serviceInterface: serviceInterface)
    return endpoint
}

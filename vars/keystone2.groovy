import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic


/**
 * Retrieve Kystone token
 * Usage:
 *     keystone.token(url: 'http://keystone',
 *                    creds: 'keystone-user-pass-creds')
 *
 * @param url
 * @param creds
 * @param project
 * @param domain
 * @param retryCount
 * @param retryTimeout
 */
def token(Map map) {

    if (!map.containsKey('url')) {
        error("Must provide Keystone URL 'url'")

    } else if (!map.containsKey('creds')) {
        error("Must provide Keystone credentials 'creds'")

    } else if (!map.containsKey('project')) {
        error("Must provide Keystone project 'project'")

    } else if (!map.containsKey('domain')) {
        error("Must provide Keystone domain 'domain'")
    }


    // optional with defaults
    def retryCount = map.retryCount ?: 3.toInteger()
    def retryTimeout = map.retryTimeout ?: 120.toInteger()


    withCredentials([[$class: "UsernamePasswordMultiBinding",
                      credentialsId: map.creds,
                      usernameVariable: "USER",
                      passwordVariable: "PASS"]]) {
        map.user = USER
        map.password = PASS
    }

    def req = ["auth": [
               "identity": [
                 "methods": ["password"],
                 "password": [
                   "user": ["name": map.user,
                            "domain": ["id": "default"],
                            "password": map.password ]]],
               "scope": ["project":
                          ["name": map.project,
                           "domain": ["id": map.domain]]]]]

    def jreq = new JsonOutput().toJson(req)

    retry (retryCount) {
        try {
            def res = httpRequest(url: map.url + "/v3/auth/tokens",
                                  contentType: "APPLICATION_JSON",
                                  httpMode: "POST",
                                  quiet: true,
                                  requestBody: jreq)

            print "Keystone token request succeesful: ${res.status}"
            return res.getHeaders()["X-Subject-Token"][0]

        } catch (err) {
            print "Keystone token request failed: ${err}"
            sleep retryTimeout
            error(err)
        }
    }
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

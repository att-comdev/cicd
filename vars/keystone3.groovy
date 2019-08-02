import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic


/**
 * Retrieve Keystone token
 * Usage:
 *     keystone.token(keystoneUrl: 'http://keystone',
 *                    keystoneCreds: 'keystone-user-pass-creds')
 *
 * @param keystoneUrl
 * @param keystoneCreds
 * @param keystoneDomain
 * @param retryCount
 * @param retryTimeout
 */
def token(Map map) {

    if (!map.containsKey('keystoneUrl')) {
        error("Must provide Keystone URL 'keystoneUrl'")

    } else if (!map.containsKey('keystoneCreds')) {
        error("Must provide Keystone credentials 'keystoneCreds'")
    }

    // optional with defaults
    def retryCount = map.retryCount ?: 5.toInteger()
    def retryTimeout = map.retryTimeout ?: 120.toInteger()
    def keystoneDomain = map.keystoneDomain ?: "default"

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
                                "domain": ["name": keystoneDomain],
                                "password": map.keystonePassword ]]]]]

    // Domain-scoped token if keystoneDomain is specificed as anything other than 'default'.
    // If a project-scoped token would ever need to be requested, this functionality will need to
    // be expanded out into specific requests, based on the user's specification, rather than the
    // assumption of either one or the other with the current implementation.
    if(keystoneDomain != "default") {
        req = ["auth": [
                    "identity": [
                    "methods": ["password"],
                    "password": [
                        "user": ["name": map.keystoneUser,
                                "domain": ["name": keystoneDomain],
                                "password": map.keystonePassword ]]],
                    "scope": [
                        "domain": ["name": keystoneDomain]]]]
    }

    def jreq = new JsonOutput().toJson(req)

    retry(retryCount) {
        try {
            def res = httpRequest(url: map.keystoneUrl + "/v3/auth/tokens",
                                  contentType: "APPLICATION_JSON",
                                  httpMode: "POST",
                                  quiet: true,
                                  requestBody: jreq)
            print "Keystone token request succeesful: ${res.status}"
            return res.getHeaders()["X-Subject-Token"][0]
        } catch(error) {
            print "Keystone token request failed: ${error}"
            sleep retryTimeout
            throw error
        }
    }
}

/**
 * Retrieve details pertaining to the passed in token
 * Usage:
 *     keystone.token(keystoneUrl: 'http://keystone',
 *                    keystoneCreds: 'keystone-user-pass-creds')
 *
 * @param keystoneUrl
 * @param keystoneCreds
 * @param keystoneDomain
 * @param keystoneToken
 */
def getTokenDetails(Map map) {

    if (!map.containsKey('keystoneToken')) {
        error("Must provide Keystone Token 'keystoneToken'")
    }

    def authToken = ""
    try {
        authToken = token(keystoneUrl: map.keystoneUrl,
                          keystoneCreds: map.keystoneCreds,
                          keystoneDomain: map.keystoneDomain,
                          retryCount: 1)
    } catch(error) {
        print "Keystone token request failed: ${error}"
    }

    def res = httpRequest(url: map.keystoneUrl + "/v3/auth/tokens",
                          httpMode: "GET",
                          customHeaders: [[name: "X-Auth-Token", value: authToken],
                                          [name: "X-Subject-Token", value: map.keystoneToken]],
                          quiet: true,
                          validResponseCodes: '200:503')
    return res
}

/**
 * Retrieve service id from Keystone for given service type
 * Usage:
 *     keystone.getServiceId(keystoneUrl: 'http://keystone',
 *                           token: 'token',
 *                           serviceType: 'shipyard')
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
    def retryCount = map.retryCount ?: 5.toInteger()
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
 *                                 serviceId: 'serviceId',
 *                                 serviceInterface: 'public')
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
    def retryCount = map.retryCount ?: 5.toInteger()
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
            print "Failed to get endpoint for service ${map.serviceId}: ${err}"
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
 *                                 serviceType: 'shipyard',
 *                                 serviceInterface: 'public')
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

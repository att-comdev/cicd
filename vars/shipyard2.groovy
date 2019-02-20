import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

/**
 * Helper for print error from failed request
 * @param code Expected response code
 * @param res Response object
 */
def _printError(code, res) {
    if (res.status != code) {
        print "See details content: " + res.content
        error("Request failed with ${res.status}")
    }
}

/**
 * Creation of "configdocs" against a site's Deckhand,
 * utilizing the Shipyard API, that will be used to deploy the
 * rest of the site.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param token An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param filePath A path to the file that you're looking to add to this collection of documents.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param bucketName The name given to the collection of documents you're creating - typically matches the site name.
 * @param bufferMode Indicates how the existing Shipyard Buffer should be handled - see: https://shipyard.readthedocs.io/en/latest/API.html?highlight=bufferMode for further details.
 */
def _createConfigdocs(uuid, token, filePath, shipyardUrl, bucketName, bufferMode) {
    def res = null
    retry(3) {
        try {
            res = httpRequest (url: shipyardUrl + "/configdocs/${bucketName}?buffermode=${bufferMode}",
                                  httpMode: "POST",
                                  customHeaders: [[name: "Content-Type", value: "application/x-yaml"],
                                                  [name: "X-Auth-Token", value: token],
                                                  [name: "X-Context-Marker", value: uuid]],
                                  quiet: true,
                                  requestBody: filePath,
                                  validResponseCodes: '200:503')
            _printError(201, res)
        } catch (err) {
                sleep 120
                error(err.getMessage())
        }
    }
    return res
}

/**
 * Commitment of "configdocs" against a site's Deckhand,
 * utilizing the Shipyard API, that will be used to deploy
 * the rest of the site.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param token An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param shipyarUrl The Shipyard URL of the site you are creating documents against.
 */
def commitConfigdocs(uuid, token, shipyardUrl) {
    def res = null
    retry(3) {
        try {
            res = httpRequest(url: shipyardUrl + "/commitconfigdocs",
                              httpMode: "POST",
                              customHeaders: [[name: "X-Auth-Token", value: token],
                                              [name: "X-Context-Marker", value: uuid]],
                              quiet: true,
                              validResponseCodes: '200:503')
            _printError(200, res)
        } catch (err) {
                sleep 120
                error(err.getMessage())
        }
    }
    return res
}


/**
 * The creation of a Shipyard, utilizing its API,
 * against a site.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param token An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param shipyardUrl The Shipyard FQDN of the site you are creating documents against.
 * @param action The action to perform - see: https://shipyard.readthedocs.io/en/latest/API_action_commands.html?highlight=action for further details.
 */
def createAction(uuid, token, shipyardUrl, action) {

    def req = ["name": action]
    def jreq = new JsonOutput().toJson(req)
    def res = null

    retry(3) {
        try {
            res = httpRequest(url: shipyardUrl + "/actions?allow-intermediate-commits=true",
                              httpMode: "POST",
                              customHeaders: [[name: "Content-Type", value: "application/json"],
                                              [name: "X-Auth-Token", value: token],
                                              [name: "X-Context-Marker", value: uuid]],
                              quiet: true,
                              requestBody: jreq,
                              validResponseCodes: '200:503')
            _printError(201, res)
        } catch (err) {
                sleep 120
                error(err.getMessage())
        }
    }
    return res
}

/**
 * Getter of shipyard action.
 *
 * @param action Shipyard action.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone or keystone password.
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 * @return List of steps for given shipyard action.
 */
def _getAction(action, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true) {
    def req = keystone.retrieveToken(keystoneCredId, keystoneUrl, withCreds)
    def token = req.getHeaders()["X-Subject-Token"][0]
    def res = null
    retry(3) {
        try {
            res = httpRequest (url: shipyardUrl + "/actions/${action}",
                               contentType: "APPLICATION_JSON",
                               httpMode: "GET",
                               quiet: true,
                               customHeaders: [[name: "X-Auth-Token", value: token]],
                               validResponseCodes: '200:503')
            _printError(200, res)
        } catch (err) {
                sleep 120
                error(err.getMessage())
        }
    }

    if (res.status != 200) {
        error("Failed to get Shypyard action steps: ${res.status}")
    }

    def cont = new JsonSlurperClassic().parseText(res.content)
    return cont
}


/**
 * Getter of steps for given shipyard action.
 *
 * @param action Shipyard action.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone or keystone password.
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 * @return List of steps for given shipyard action.
 */
def getSteps(action, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true) {
    action = _getAction(action, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true)
    return action.steps
}


/**
 * Gets state for given step.
 *
 * @param systep Step from shipyard action.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone or keystone password.
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 * @return state State of the step (such as null, "success", "skipped", "running", "queued", "scheduled")
 */
def getState(systep, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true) {
    def req = keystone.retrieveToken(keystoneCredId, keystoneUrl, withCreds)
    def token = req.getHeaders()["X-Subject-Token"][0]
    def res = null
    retry(3) {
        try {
            res = httpRequest (url: shipyardUrl + "${systep.url}",
                                   contentType: "APPLICATION_JSON",
                                   httpMode: "GET",
                                   quiet: true,
                                   customHeaders: [[name: "X-Auth-Token", value: token]],
                                   validResponseCodes: '200:503')
            _printError(200, res)
        } catch (err) {
                sleep 120
                error(err.getMessage())
        }
    }

    if (!res) {
        error("httpRequest returned null - likely library issue")
    }

    if (res.status != 200) {
        error("Failed to get Shipyard step info: ${res.status}")
    }

    if (!res.content) {
        error("Shypyard returned null content")
    }

    def cont = new JsonSlurperClassic().parseText(res.content)
    print cont
    print res.status
    print res.content
    state = cont.state

    return state
}

/**
 * The creation of a Shipyard config
 * Archive with yaml config should have a name 'site-config.tar.gz'
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param token An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param artfPath Artifactory path for executed pipeline.
 * @param siteName Site name for executed pipeline.
 */
def createConfigdocs(uuid, token, shipyardUrl, siteName) {
    def manifests = ""
    files = findFiles(glob: "${siteName}/*.yaml")
    files.each {
            print "Reading file -> ${it}"
            manifests += readFile it.path
    }

    _createConfigdocs(uuid, token, manifests, shipyardUrl, siteName, "replace")
}

/**
 * Helper method for waiting of step became in 'success' or 'skipped' state.
 *
 * @param systep Step from shipyard action.
 * @param interval Interval in second for sleep between attepmts.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone or keystone password.
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 */
def waitStep(systep, interval, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true) {

    print ">> Waiting on Shipyard step: ${systep}"

    def String state = systep.state

    while (state == null || state == "running" || state == "queued" || state == "scheduled" || state == "up_for_retry") {
        sleep interval

        retry (3) {
            state = getState(systep, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
        }
    }

    if (state != "success" && state != "skipped") {
        error("Failed Shipyard task ${systep.id}")
    }
}


/**
 * Helper method for waiting steps for given shipyard action.
 * Waits each step to become in success or skipped status.
 *
 * @param action Shipyard action id.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone or keystone password.
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 */
def waitActionSteps(action, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true) {

    def systeps = getSteps(action, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)

    systeps.each() {
        if (it.id == "drydock_build" || it.id == "armada_build") {
            stage ("Shipyard (${it.id})") {
                waitStep(it, 240, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
            }
        } else {
            waitStep(it, 4, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
        }
    }
}


/**
 * Upload config
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param token An authorization token retrieved from Keystone prior to calling this function that may allow you to perform this action.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param artfPath Artifactory path for executed pipeline.
 * @param siteName Site name for executed pipeline.
 */
def uploadConfig(uuid, token, shipyardUrl, siteName) {

    stage('Shipyard Config Create') {
        createConfigdocs(uuid, token, shipyardUrl, siteName)
    }

    stage('Shipyard Config Commit') {
        commitConfigdocs(uuid, token,  shipyardUrl)
    }
}

/**
 * Helper method for action steps info.
 *
 *  @param action Shipyard action from requet json.
 */
def _printActionSteps(action) {
    def steps = action.steps
    def status = []
    def failed = []
    steps.each() {
        status += "${it.id}(${it.index}): ${it.state} state"
        if (it.state == "failed") {
            failed += "${it.id}"
        }
    }
    print status.join("\n")
    return failed
}


/**
 * Helper method for waiting shipyard action in one of the finished states (Failed, Paused, Complete).
 * Waits each action to become in Failed/Paused/Complete status
 *
 * @param action Shipyard action.
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone or keystone password.
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 */
def waitAction(action, uuid, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true) {

    def actionId
    stage('Action create') {
        def req = keystone.retrieveToken(keystoneCredId, keystoneUrl, withCreds)
        def token = req.getHeaders()["X-Subject-Token"][0]
        def res = createAction(uuid, token, shipyardUrl, action)
        def cont = new JsonSlurperClassic().parseText(res.content)
        actionId = cont.id
    }
    action = _getAction(actionId, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
    def String status = action.action_lifecycle
    def failedSteps = []

    while (status == "Pending" || status == "Processing") {
        sleep 240

        action = _getAction(actionId, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
        status = action.action_lifecycle
        print "Wait until action is complete. Currently in ${status} state."
        failedSteps = _printActionSteps(action)
    }

    if (status != "Complete" || failedSteps) {
        error("Shipyard action finished with status ${status} instead of complete. (${failedSteps})")
    }
}

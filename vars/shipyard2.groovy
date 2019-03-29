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
 * @param parameters Optional map of parameters needed to create action
 */
def createAction(uuid, token, shipyardUrl, action, parameters = null) {

    def req = ["name": action]
    if (parameters) {
        req = req.plus("parameters": parameters)
    }
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
    def running = []
    steps.each() {
        status += "${it.id}(${it.index}): ${it.state} state"
        if (it.state == "failed") {
            failed += "${it.id}"
        }
        if (it.state == "running") {
            running += "${it.id}"
        }
    }
    print status.join("\n")
    return [failed, running]
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
 * @param parameters Optional map of parameters needed to create action
 * @param genesisCreds Jenkins creds name for debug kubectl command execution during action check
 * @param genesisIp IP address of genesis node for debug kubectl command execution
 */
def waitAction(action, uuid, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true, parameters = null, genesisCreds=null, genesisIp=null) {

    def actionId
    stage('Action create') {
        def req = keystone.retrieveToken(keystoneCredId, keystoneUrl, withCreds)
        def token = req.getHeaders()["X-Subject-Token"][0]
        def res = createAction(uuid, token, shipyardUrl, action, parameters)
        def cont = new JsonSlurperClassic().parseText(res.content)
        actionId = cont.id
    }
    action = _getAction(actionId, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
    def String status = action.action_lifecycle
    def failedSteps = []
    def runningSteps = []
    def stages = []
    // We would like to skip stage creation for some steps since they have subtusks
    // and stages will be created for them.
    def skipSteps = ['drydock_build', 'armada_build']

    while (status == "Pending" || status == "Processing") {
        sleep 240

        action = _getAction(actionId, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
        status = action.action_lifecycle
        print "Wait until action is complete. Currently in ${status} state."
        (failedSteps, runningSteps) = _printActionSteps(action)
        if (failedSteps) {
            stageName = failedSteps.join(",")
            stage(stageName) {
                error("Step ${failedSteps} failed")
            }
        }
        def debug = true
        runningSteps.each() {
            if ( !(it in stages) & !(it.toString() in skipSteps)) {
                stages += it
                stage "Step ${it}"
                // In case of few steps in running state we may get a situation when few
                // stages were created at the same time and only last will be closed for next
                //stage creation. All other stages will be in running state until job is finished.
                // Add sleep to fix this issue with hanging stages.
                sleep 5
            }
            // Execute kubectl debug command only once for all running steps for each action check.
            // For drydock_build step check nodes state. For all other check pods state.
            if (genesisCreds && genesisIp && debug) {
                if (it.toString() == 'drydock_build') {
                    ssh.cmd (genesisCreds, genesisIp, 'sudo kubectl get nodes')
                    debug = false
                } else {
                    ssh.cmd (genesisCreds, genesisIp, 'sudo kubectl get pods --all-namespaces | grep -vE "Completed|Running"')
                    debug = false
                }
            }
        }
    }

    if (status != "Complete") {
        error("Shipyard action finished with status ${status} instead of complete.")
    }
}

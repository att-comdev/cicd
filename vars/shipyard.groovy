import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

/**
 * Creation of "configdocs" against a site's Deckhand,
 * utilizing the Shipyard CLI, that will be used to deploy
 * the rest of the site. This function should only be called
 * within a Shipyard CLI container.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param bucketName The name given to the collection of documents you're creating - typically matches the site name.
 */
def createConfigdocsWithinContainer(uuid, bucketName) {
    sh """
        shipyard --context-marker=${uuid} create configdocs ${bucketName} --replace --directory=${bucketName} > response
        cat response
    """

    def errors = getShipyardErrorCount() as Integer
    println "Shipyard Errors: " + errors
    if(errors.compareTo(0) == 1) {
        error("Build failed. Error(s) during Shipyard processing.")
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
def createConfigdocs(uuid, token, filePath, shipyardUrl, bucketName, bufferMode) {
    def res = null
    retry(3) {
        try {
            res = httpRequest (url: shipyardUrl + "/api/v1.0/configdocs/${bucketName}?buffermode=${bufferMode}",
                                  httpMode: "POST",
                                  customHeaders: [[name: "Content-Type", value: "application/x-yaml"],
                                                  [name: "X-Auth-Token", value: token],
                                                  [name: "X-Context-Marker", value: uuid]],
                                  quiet: true,
                                  requestBody: filePath)
        } catch (err) {
                sleep 120
                print "Status: " + res.status
                print "Content: " + res.content
                error(err)
        }
    }
    return res
}

/**
 * Commitment of "configdocs" against a site's Deckhand,
 * utilizing the Shipyard CLI, that will be used to deploy
 * the rest of the site. This function should only be called
 * within a Shipyard CLI container.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 */
def commitConfigDocsWithinContainer(uuid) {
    sh """
        shipyard --context-marker=${uuid} commit configdocs > response
        cat response
    """
    def errors = getShipyardErrorCount() as Integer
    println "Shipyard Errors: " + errors
    if(errors.compareTo(0) == 1) {
        error("Build failed. Error(s) during Shipyard processing.")
    }
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
            res = httpRequest(url: shipyardUrl + "/api/v1.0/commitconfigdocs",
                              httpMode: "POST",
                              customHeaders: [[name: "X-Auth-Token", value: token],
                                              [name: "X-Context-Marker", value: uuid]],
                              quiet: true)
        } catch (err) {
                sleep 120
                print "Status: " + res.status
                print "Content: " + res.content
                error(err)
        }
    }
    return res
}

/**
 * The creation of a Shipyard action, utilizing the CLI,
 * against a site. This function should only be called
 * within a Shipyard CLI container.
 *
 * @param uuid A pre-generated uuid that helps to tie a series of requests together across software components.
 * @param action The action to perform - see: https://shipyard.readthedocs.io/en/latest/API_action_commands.html?highlight=action for further details.
 */
def createActionWithinContainer(uuid, action) {
    return sh(script: "shipyard --context-marker=${uuid} create action ${action} --allow-intermediate-commits", returnStdout: true).trim()
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
            res = httpRequest(url: shipyardUrl + "/api/v1.0/actions?allow-intermediate-commits=true",
                              httpMode: "POST",
                              customHeaders: [[name: "Content-Type", value: "application/json"],
                                              [name: "X-Auth-Token", value: token],
                                              [name: "X-Context-Marker", value: uuid]],
                              quiet: true,
                              requestBody: jreq)
        } catch (err) {
                sleep 120
                print "Status: " + res.status
                print "Content: " + res.content
                error(err)
        }
    }
    return res
}

/**
 * Helper method to read the Shipyard CLI written
 * response to see if any errors were returned. This
 * is only necessary at this time because Shipyard
 * does not return a non-zero exit code when errors
 * are returned.
 *
 * @return String the number of errors returned by the Shipyard CLI request
 */
private def getShipyardErrorCount() {
    return sh(script: "tail -1 response | cut -d ':' -f2 | xargs | cut -d ',' -f1", returnStdout: true)
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
    def req = keystone.retrieveToken(keystoneCredId, keystoneUrl, withCreds)
    def token = req.getHeaders()["X-Subject-Token"][0]
    def res = null
    retry(3) {
        try {
            res = httpRequest (url: shipyardUrl + "/api/v1.0/actions/${action}",
                               contentType: "APPLICATION_JSON",
                               httpMode: "GET",
                               quiet: true,
                               customHeaders: [[name: "X-Auth-Token", value: token]])
        } catch (err) {
                sleep 120
                print "Status: " + res.status
                print "Content: " + res.content
                error(err)
        }
    }

    if (res.status != 200) {
        error("Failed to get Shypyard action steps: ${res.status}")
    }

    def cont = new JsonSlurperClassic().parseText(res.content)
    print cont

    return cont.steps
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
            res = httpRequest (url: shipyardUrl + "/api/v1.0${systep.url}",
                                   contentType: "APPLICATION_JSON",
                                   httpMode: "GET",
                                   quiet: true,
                                   customHeaders: [[name: "X-Auth-Token", value: token]])
        } catch (err) {
                sleep 120
                print "Status: " + res.status
                print "Content: " + res.content
                error(err)
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
def createConfig(uuid, token, shipyardUrl, artfPath, siteName) {
    artifactory.download("${artfPath}/site-config.tar.gz", "")
    sh "sudo rm -rf ${siteName}"
    sh "tar xzf site-config.tar.gz"

    def manifests = ""
    files = findFiles(glob: "${siteName}/*.yaml")
    files.each {
            print "Reading file -> ${it}"
            manifests += readFile it.path
    }

    createConfigdocs(uuid, token, manifests, shipyardUrl, siteName, "replace")
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

    while (state == null || state == "running" || state == "queued" || state == "scheduled") {
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
 * Waits ech step to become in success or skipped status.
 *
 * @param action Shipyard action id.
 * @param shipyardUrl The Shipyard URL of the site you are creating documents against.
 * @param keystoneCredId The ID of the credential (user+pass) established within Jenkins to authenticate against a site's Keystone or keystone password.
 * @param keystoneUrl The IAM URL of the site you are authenticating against.
 * @param withCreds Boolean. Flag for using jenkins configuration to get keystone credentials.
 */
def waitSteps(action, shipyardUrl, keystoneCredId, keystoneUrl, withCreds=true) {

    def systeps = getSteps(action, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)

    systeps.each() {
        if (it.id == "drydock_build" || it.id == "armada_build") {
           waitStep(it, 240, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
        } else {
            waitStep(it, 4, shipyardUrl, keystoneCredId, keystoneUrl, withCreds)
        }
    }
}

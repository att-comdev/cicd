import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput


/**
 * Helper for print error from failed request
 * @param code Expected response code
 * @param res Response object
 */
def _printError(code, res) {
    if( !(code instanceof List)) {
        code = [code]
    }
    if ( !(code.contains(res.status))) {
        print "See details content: " + res.content
        error("Request failed with ${res.status}")
    }
}

def getBasicAuth (String user, String passwd) {
    def creds = "${user}:${passwd}"
    return creds.bytes.encodeBase64().toString()
}

def getSystemPath (String ip, String auth) {
    def res
    retry(5) {
        try {
            res = httpRequest (url: "https://${ip}/redfish/v1/Systems/",
                               customHeaders:[[name:'Authorization', value:"Basic ${auth}"]],
                               httpMode: 'GET',
                               ignoreSslErrors: true)

            _printError(200, res)
        } catch (err) {
            print "Failed to get System Path: ${err}"
            sleep 120
            error(err.getMessage())
        }
    }

    def cont = new JsonSlurperClassic().parseText(res.content)
    def systemPath = cont.Members[0]."@odata.id"

    if (systemPath.endsWith("/")) {
        systemPath = systemPath[0..-2]
    }

    print "System Path: " + systemPath
    return systemPath
}

def getPowerState (String ip, String auth) {

    def systemPath = getSystemPath(ip, auth)

    def res
    retry(5) {
        try {
            res = httpRequest (url: "https://${ip}${systemPath}/",
                               customHeaders:[[name:'Authorization', value:"Basic ${auth}"]],
                               httpMode: 'GET',
                               ignoreSslErrors: true)
            _printError(200, res)
        } catch (err) {
            print "Failed to get power state: ${err}"
            sleep 120
            error(err.getMessage())
        }
    }

    def cont = new JsonSlurperClassic().parseText(res.content)
    print "PowerState: ${cont.PowerState}"

    return cont.PowerState
}

def setPowerState (String ip, String auth, String state) {

    def systemPath = getSystemPath(ip, auth)

    def req = [ 'ResetType': state ]
    def jreq = new JsonOutput().toJson(req)

    def res
    retry(5) {
        try {
            res = httpRequest (url: "https://${ip}${systemPath}/Actions/ComputerSystem.Reset/",
                               customHeaders:[[name:'Authorization', value:"Basic ${auth}"]],
                               httpMode: 'POST',
                               ignoreSslErrors: true,
                               contentType: 'APPLICATION_JSON',
                               requestBody: jreq)

            // 204 iDrac, 200 iLO
            _printError([200, 204], res)
        } catch (err) {
            print "Failed to set power state: ${err}"
            sleep 120
            error(err.getMessage())
        }
    }
}

def powerOn (String ip, String auth) {

    if (getPowerState(ip, auth) == 'On') {
        print "Power state already On, skipping"
        return 0
    }

    print "Setting power state On for node ${ip}"
    setPowerState(ip, auth, 'On')

    timeout(1) {
        def state = getPowerState(ip, auth)
        while (state != 'On') {
            print "Power state is not yet \'On\' (${state}), waiting 10 seconds"
            sleep 10
            state = getPowerState(ip, auth)
        }
    }
}

def powerOff (String ip, String auth) {

    if (getPowerState(ip, auth) == 'Off') {
        print "Power state already Off, skipping"
        return 0
    }

    print "Setting power state ForceOff for node ${ip}"
    setPowerState(ip, auth, 'ForceOff')

    timeout(1) {
        def state = getPowerState(ip, auth)
        while (state != 'Off') {
            print "Power state is not yet \'Off\' (${state}), waiting 10 seconds"
            sleep 10
            state = getPowerState(ip, auth)
        }
    }
}

def powerReset (String ip, String auth) {
    // ForceRestart is not an option in iDrac 8, changing
    // powerReset to 'ForceOff' -> 'On"'
    print "Beginning power reset process for node ${ip}"
    powerOff(ip, auth)
    powerOn(ip, auth)
}

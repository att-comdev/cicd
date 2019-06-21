import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput


def getBasicAuth (String user, String passwd) {
    def creds = "${user}:${passwd}"
    return creds.bytes.encodeBase64().toString()
}

def getSystemPath (String ip, String auth, String proxy="") {

    def res = httpRequest (url: "https://${ip}/redfish/v1/Systems/",
                  customHeaders:[[name:'Authorization', value:"Basic ${auth}"]],
                  httpMode: 'GET',
                  ignoreSslErrors: true,
                  httpProxy: proxy)

    if (res.status != 200) {
          error("Failed to get System Path: ${res.status}, ${res.content}")
    }

    def cont = new JsonSlurperClassic().parseText(res.content)
    print "System Path: " + cont.Members[0]."@odata.id"

    return cont.Members[0]."@odata.id"
}

def getPowerState (String ip, String auth, String proxy="") {

    def systemPath = getSystemPath(ip, auth, proxy)

    def res = httpRequest (url: "https://${ip}${systemPath}",
                  customHeaders:[[name:'Authorization', value:"Basic ${auth}"]],
                  httpMode: 'GET',
                  ignoreSslErrors: true,
                  httpProxy: proxy)

    if (res.status != 200) {
          error("Failed to get power state: ${res.status}, ${res.content}")
    }

    def cont = new JsonSlurperClassic().parseText(res.content)
    print "PowerState: ${cont.PowerState}"

    return cont.PowerState
}

def setPowerState (String ip, String auth, String state, String proxy="") {

    def systemPath = getSystemPath(ip, auth, proxy)

    def req = [ 'ResetType': state ]
    def jreq = new JsonOutput().toJson(req)

    res = httpRequest (url: "https://${ip}${systemPath}/Actions/ComputerSystem.Reset",
              customHeaders:[[name:'Authorization', value:"Basic ${auth}"]],
              httpMode: 'POST',
              ignoreSslErrors: true,
              contentType: 'APPLICATION_JSON',
              requestBody: jreq,
              httpProxy: proxy)

    if (res.status != 204) {
         error("Failed to set power state: ${res.status}, ${res.content}")
    }
}

def powerOn (String ip, String auth, String proxy="") {

    if (getPowerState(ip, auth, proxy) == 'On') {
        print "Power state already On, skipping"
        return 0
    }

    print "Setting power state On for node ${ip}"
    setPowerState(ip, auth, 'On', proxy)

    timeout(1) {
        def state = getPowerState(ip, auth, proxy)
        while (state != 'On') {
            print "Power state is not yet \'On\' (${state}), waiting 10 seconds"
            sleep 10
            state = getPowerState(ip, auth, proxy)
        }
    }
}

def powerOff (String ip, String auth, String proxy="") {

    if (getPowerState(ip, auth, proxy) == 'Off') {
        print "Power state already Off, skipping"
        return 0
    }

    print "Setting power state ForceOff for node ${ip}"
    setPowerState(ip, auth, 'ForceOff', proxy)

    timeout(1) {
        def state = getPowerState(ip, auth, proxy)
        while (state != 'Off') {
            print "Power state is not yet \'Off\' (${state}), waiting 10 seconds"
            sleep 10
            state = getPowerState(ip, auth, proxy)
        }
    }
}

def powerReset (String ip, String auth, String proxy="") {

    print "Setting power state ForceRestart for node ${ip}"
    setPowerState(ip, auth, 'ForceRestart', proxy)

    timeout(1) {
        def state = getPowerState(ip, auth, proxy)
        while (state != 'On') {
            print "Power state is not yet \'On\' (${state}), waiting 10 seconds"
            sleep 10
            state = getPowerState(ip, auth, proxy)
        }
    }
}

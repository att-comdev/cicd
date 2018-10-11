import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput


def getBasicAuth (String user, String passwd) {
    def creds = "${user}:${passwd}"
    return creds.bytes.encodeBase64().toString()
}

def getPowerState (String ip, String auth) {

    def res = httpRequest (url: "https://${ip}/redfish/v1/Systems/System.Embedded.1/",
                  customHeaders:[[name:'Authorization', value:"Basic ${auth}"]],
                  httpMode: 'GET',
                  ignoreSslErrors: true)

    if (res.status != 200) {
          error("Failed to get power state: ${res.status}, ${res.content}")
    }

    def cont = new JsonSlurperClassic().parseText(res.content)
    print "PowerState: ${cont.PowerState}"

    return cont.PowerState
}

def setPowerState (String ip, String auth, String state) {

    def req = [ 'ResetType': state ]
    def jreq = new JsonOutput().toJson(req)

    res = httpRequest (url: "https://${ip}/redfish/v1/Systems/System.Embedded.1/Actions/ComputerSystem.Reset",
              customHeaders:[[name:'Authorization', value:"Basic ${auth}"]],
              httpMode: 'POST',
              ignoreSslErrors: true,
              contentType: 'APPLICATION_JSON',
              requestBody: jreq)

    if (res.status != 204) {
         error("Failed to set power state: ${res.status}, ${res.content}")
    }
}


def powerOn (String ip, String auth) {

    if (getPowerState(ip, auth) == 'On') {
        print "Power state already On, skipping.."
        retun 0
    }

    setPowerState(ip, auth, 'On')

    timeout(1) {
        while (getPowerState(ip, auth) != 'On') {
            sleep 10
        }
    }
}

def powerOff (String ip, String auth) {

    if (getPowerState(ip, auth) == 'Off') {
        print "Power state already Off, skipping.."
        retun 0
    }

    setPowerState(ip, auth, 'ForceOff')

    timeout(1) {
        while (getPowerState(ip, auth) != 'Off') {
            sleep 10
        }
    }
}


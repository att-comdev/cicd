
import groovy.json.JsonOutput


def create(String url, String creds, String userData, String heatTemplate, String project) {

    token = keystone2.token(keystoneUrl: url, keystoneCreds: creds)
    endpoint = "https://orchestration-nc.mtn13b2.cci.att.com:443/v1/${project}/stacks"

    ud = readFile file: userData
    tmpl = readFile file: heatTemplate

    spec = ['files': ['bootstrap.sh': ud],
            'stack_name': 'ks3019-test-stack',
            'template': tmpl]

    jreq = new JsonOutput().toJson(spec)

   def res = httpRequest (url: endpoint,
                              httpMode: "POST",
                              contentType: "APPLICATION_JSON",
                                customHeaders: [[name: "X-Auth-Token", value: token]],
                              requestBody: jreq,
                              quiet: false)

    print output

}


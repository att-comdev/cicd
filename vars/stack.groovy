
import groovy.json.JsonOutput


def create(String url, String creds, String userData, String heatTemplate) {

    token = keystone2.token(keystoneUrl: url, keystoneCreds: creds)
    print token
    endpoint = keystone2.getServiceEndpoint(keystoneUrl: url, token: token, serviceType: 'orchestration')

    ud = readFile file: userData
    tmpl = readFile file: heatTemplate

    spec = ['files': ['bootstrap.sh': ud],
            'stack_name': 'ks3019-test-stack',
            'template': tmpl]

    output = new JsonOutput().toJson(spec)

    print output


}


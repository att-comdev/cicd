
def create(String url, String creds, String userData, String heatTemplate) {

    keystone2.token(keystoneUrl: url, keystoneCreds: creds)

    def ud = readFile file: userData,
    def tmpl = readFile file: heatTemplate

    print ud
    print tmpl

    // read user data
    // set parameters
    // read template
}


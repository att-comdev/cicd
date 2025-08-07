import com.att.nccicd.config.conf as config
conf = new config(env).CONF

REFS = "+refs/heads/*:refs/remotes/origin/* +refs/changes/*:refs/changes/*"
REQUIREMENT_REPO = "mos-requirements"

currentBuild.displayName = "#${BUILD_NUMBER} ${PROJECT_NAME}"

IMAGE = "cicd-ubuntu-18.04-server-cloudimg-amd64"
TOX_CHECK = 'OS_LOG_PATH=.; tox'
if ( [conf.YOGA_BRANCH, conf.WALLABY_BRANCH, conf.XENA_BRANCH].contains(PROJECT_BRANCH) ) {
    IMAGE = "cicd-ubuntu-20.04-server-cloudimg-amd64"
}
if ( [conf.ANTELOPE_BRANCH, conf.CARACAL_BRANCH].contains(PROJECT_BRANCH) ) {
    IMAGE = "cicd-ubuntu-22.04-server-cloudimg-amd64"
}
if ( [conf.EPOXY_BRANCH].contains(PROJECT_BRANCH) ) {
    IMAGE = "cicd-ubuntu-24.04-server-cloudimg-amd64"
}

def compileSshData() {
    sshConfig = ""
    keys = []
    parseSshData().each { entry ->
        sshConfig += "Host $entry.value.resource\n" +
                     "User $entry.value.user\n"
        keys.add(entry.key)
    }
    return [keys, sshConfig]
}

// Parse json containing
// {'<credential_name>': {'user': '<user>', 'resource': '<resource>'}}, ...}
// The source of data what credentials to use in ssh-agent with what user
// to what resource
def parseSshData() {
    return new groovy.json.JsonSlurper().parseText(SSH_DATA)
}

// Compile ssh-agent key names and ssh config from SSH_DATA to be used
// for fetching projects to internal mirror
(KEY_NAMES, SSH_CONFIG) = compileSshData()


vm (image: IMAGE, flavor: 'm1.large') {
    stage("Setup Node") {
        writeFile file: "${HOME}/.ssh/config", text: SSH_CONFIG
        sh "sudo apt-get update"
        sh "sudo bash -c 'export DEBIAN_FRONTEND=noninteractive; " +
                         "apt-get -y install python3-pip gettext libpq-dev " +
                         "libssl-dev libsasl2-dev libldap2-dev bandit " +
                         "qemu-utils'"
        sh "sudo pip3 install --index-url ${ARTF_PIP_INDEX_URL} virtualenv " +
          "'tox<4.0.0.a2'"
        sh "sudo bash -c 'mkdir -p /opt/stack; chown ubuntu:ubuntu /opt/stack'"
    }
    stage('Project Checkout') {
        withEnv(["SHALLOW_CLONE=False"]) {
            gerrit.cloneToBranch("${INTERNAL_GERRIT_SSH}/${PROJECT_NAME}",
                                 PROJECT_REF,
                                 "test-repo",
                                 INTERNAL_GERRIT_KEY,
                                 REFS)
        }
        gerrit.cloneProject("${INTERNAL_GERRIT_SSH}/${REQUIREMENT_REPO}",
                             PROJECT_BRANCH,
                             "refs/heads/${PROJECT_BRANCH}",
                             REQUIREMENT_REPO,
                             INTERNAL_GERRIT_KEY)

    }
    def ucPath = "${WORKSPACE}/${REQUIREMENT_REPO}/upper-constraints.txt"
    stage('Tox') {
        dir("${WORKSPACE}/${REQUIREMENT_REPO}") {
            sshagent([INTERNAL_GERRIT_KEY]) {
                sh ('''mkdir /tmp/wheels;
                       virtualenv .venv;
                       . .venv/bin/activate;
                       pip install pkginfo;
                       for item in $(grep '^git+' upper-constraints.txt); do
                         pip wheel --no-deps --wheel-dir /tmp/wheels ${item}
                       done;
                       sed -i '/^git+/d' upper-constraints.txt;
                       for wheel in $(ls /tmp/wheels/*.whl); do
                         cmd=\"import pkginfo; \
                               wheel = pkginfo.Wheel('${wheel}'); \
                               msg = '{}==={}'.format(wheel.name, \
                                                      wheel.version); \
                               print(msg)\"
                         python -c "${cmd}" >> upper-constraints.txt
                       done;''')
            }
        }
        dir("${WORKSPACE}/test-repo") {
            withEnv(["UPPER_CONSTRAINTS_FILE=${ucPath}",
                     "TOX_CONSTRAINTS_FILE=${ucPath}",
                     "PIP_INDEX_URL=${ARTF_PIP_INDEX_URL}",
                     "PIP_FIND_LINKS=/tmp/wheels",
                     "HTTPS_PROXY=", "HTTP_PROXY="]) {
                sshagent([INTERNAL_GERRIT_KEY]) {
                    sh TOX_CHECK
                }
            }
        }
    }
}

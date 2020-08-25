import com.att.nccicd.config.conf as config
conf = new config(env).CONF

REFS = "+refs/heads/*:refs/remotes/origin/* +refs/changes/*:refs/changes/*"
REQUIREMENT_REPO = "mos-requirements"

currentBuild.displayName = "#${BUILD_NUMBER} ${PROJECT_NAME}"

TOX_CHECK = 'OS_LOG_PATH=.; tox -epep8,py27'
IMAGE = "cicd-ubuntu-16.04-server-cloudimg-amd64"
if (PROJECT_BRANCH != conf.OCATA_BRANCH) {
    IMAGE = "cicd-ubuntu-18.04-server-cloudimg-amd64"
    TOX_CHECK = 'OS_LOG_PATH=.; tox'
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
                         "libssl-dev libsasl2-dev libldap2-dev bandit'"
        sh "sudo pip3 install --index-url ${ARTF_PIP_INDEX_URL} tox"
        sh "sudo bash -c 'mkdir -p /opt/stack; chown ubuntu:ubuntu /opt/stack'"
    }
    stage('Project Checkout') {
        gerrit.cloneToBranch("${INTERNAL_GERRIT_SSH}/${PROJECT_NAME}",
                             PROJECT_REF,
                             "test-repo",
                             INTERNAL_GERRIT_KEY,
                             REFS)
        gerrit.cloneProject("${INTERNAL_GERRIT_SSH}/${REQUIREMENT_REPO}",
                             PROJECT_BRANCH,
                             "refs/heads/${PROJECT_BRANCH}",
                             REQUIREMENT_REPO,
                             INTERNAL_GERRIT_KEY)

    }
    def ucPath = "${WORKSPACE}/${REQUIREMENT_REPO}/upper-constraints.txt"
    stage('Tox') {
        dir("${WORKSPACE}/test-repo") {
            withEnv(["UPPER_CONSTRAINTS_FILE=${ucPath}",
                     "TOX_CONSTRAINTS_FILE=${ucPath}",
                     "PIP_INDEX_URL=${ARTF_PIP_INDEX_URL}",
                     "HTTPS_PROXY=", "HTTP_PROXY="]) {
                sshagent([INTERNAL_GERRIT_KEY]) {
                    sh TOX_CHECK
                }
            }
        }
    }
}

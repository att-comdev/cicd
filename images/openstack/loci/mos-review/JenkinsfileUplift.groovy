import com.att.nccicd.config.conf as config
import groovy.json.JsonSlurperClassic

conf = new config(env).CONF

json = new JsonSlurperClassic()

NET_RETRY_COUNT = env.NET_RETRY_COUNT.toInteger()

IMAGES_VERSIONS = json.parseText(params.IMAGES)

MANIFESTS_PROJECT_NAME = conf.GLOBAL_REPO
VERSIONS_PATH = conf.VERSIONS_PATH
MANIFESTS_BRANCH = conf.OS_RELEASE_MANIFESTS_BRANCH_MAP[RELEASE]
RELEASES_REGEX = "(${json.parseText(env.SUPPORTED_RELEASES).join("|")})"
if (TARGET_RELEASE_ONLY.toBoolean()) {
    RELEASES_REGEX = RELEASE
}

COMMIT_ARGS = ("-c 'user.name=Jenkins (${INTERNAL_GERRIT_USER})' " +
               "-c 'user.email=${INTERNAL_GERRIT_USER}@att.com'")

def compileSshData() {
    sshConfig = ""
    keys = []
    json.parseText(SSH_DATA).each { entry ->
        sshConfig += "Host ${entry.value.resource}\n" +
                     "User ${entry.value.user}\n"
        keys.add(entry.key)
    }
    return [keys, sshConfig]
}

def getProjectRepoUrl(prj) {
    return prj.contains("ssh://") ? prj : "${INTERNAL_GERRIT_SSH}/${prj}"
}


// Compile ssh-agent key names and ssh config from SSH_DATA to be used
// for fetching projects to internal mirror
(KEY_NAMES, SSH_CONFIG) = compileSshData()


vm (initScript: 'loci-bootstrap.sh',
        image: 'cicd-ubuntu-18.04-server-cloudimg-amd64',
        flavor: 'm1.medium',
        nodePostfix: '',
        doNotDeleteNode: false) {
    // Create ssh config on slave to control what login is used for
    // what resource
    writeFile file: "${HOME}/.ssh/config", text: SSH_CONFIG
    sh "sudo bash -c 'echo \"nameserver ${DNS_SERVER_TWO}\" > /etc/resolv.conf'"
    stage("Replacing images in versions.yaml") {
        env.GERRIT_TOPIC = TOPIC
        def changeId = null

        changes = gerrit.getTopicCommitInfo(
            MANIFESTS_PROJECT_NAME, INTERNAL_GERRIT_URL,
            INTERNAL_GERRIT_PORT, INTERNAL_GERRIT_KEY
        )

        if (changes.size() > 1) {
            error("Something wrong occurred. There should be either one or " +
                  "none open changes for topic ${TOPIC}.\n" +
                  "Please close extra changes.")
        }

        if (changes) {
            changeId = changes[0]["id"]
        }

        utils.retrier (NET_RETRY_COUNT) {
            gerrit.cloneToBranch(
                getProjectRepoUrl(MANIFESTS_PROJECT_NAME),
                MANIFESTS_BRANCH,
                MANIFESTS_PROJECT_NAME,
                INTERNAL_GERRIT_KEY,
                null
            )
        }
        dir(MANIFESTS_PROJECT_NAME) {
            versions = readFile VERSIONS_PATH
            versions_updated = versions
            IMAGES_VERSIONS.each { _, data ->
                image = data['url']
                comment = " ${data['vcsRef']}.${data['date']}"
                (_, replace_to, pattern) = ((image =~ /.*?\/((.*?)[@:].*)/)[0])
                // For pattern replace actual release name by regex matching any release
                imagePattern = pattern.replaceAll(RELEASES_REGEX, RELEASES_REGEX) + '[@:].*'
                imagePattern = imagePattern.replaceAll('(openstack|openstack-patchset)/',
                                                       '(openstack|openstack-patchset)/')
                versions_updated = versions_updated.replaceAll(
                    imagePattern, replace_to)
                commentPattern = "(?<=#).*\\.\\d{4}(-\\d{2}){2}_\\d{2}(-\\d{2}){2}.*(?=\n.{0,100}${replace_to})"
                versions_updated = versions_updated.replaceAll(
                    commentPattern, comment)
            }
            if(versions_updated == versions) {
                print "No new changes. Versions.yaml is up to date."
            } else {
                writeFile file: VERSIONS_PATH, text: versions_updated
                sshagent([INTERNAL_GERRIT_KEY]) {
                    sh ("scp -p -P ${INTERNAL_GERRIT_PORT} " +
                        "${INTERNAL_GERRIT_URL}:hooks/commit-msg " +
                        ".git/hooks/")
                    msg = params.COMMIT_MESSAGE + "\n\n${env.BUILD_URL}"
                    if (changeId) {
                        msg += "\n\nChange-Id: ${changeId}"
                    }
                    sh "git ${COMMIT_ARGS} commit -a -m '${msg}'"
                    sh "git show HEAD"
                    sh "git push origin HEAD:refs/for/${MANIFESTS_BRANCH}%topic=${TOPIC}"
                }
            }
        }
    }
}

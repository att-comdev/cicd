import com.att.nccicd.config.conf as config
import groovy.json.JsonSlurperClassic

conf = new config(env).CONF

json = new JsonSlurperClassic()

NET_RETRY_COUNT = env.NET_RETRY_COUNT.toInteger()
TOPIC = "${RELEASE}-mirrors-update"
COMMIT_ARGS = "-c 'user.name=Jenkins (${INTERNAL_GERRIT_USER})' -c 'user.email=${INTERNAL_GERRIT_USER}@att.com'"
MESSAGE = "[Update] Mirrors revisions update"

RELEASE_BRANCH_MAP = json.parseText(RELEASE_BRANCH_MAP)
BRANCH = RELEASE_BRANCH_MAP[RELEASE]

MIRRORS_BRANCH = 'MOS/' + RELEASE


def getProjectRepoUrl(prj) {
    return prj.contains("ssh://") ? prj : "${INTERNAL_GERRIT_SSH}/${prj}"
}

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

// Compile ssh-agent key names and ssh config from SSH_DATA to be used
// for fetching projects to internal mirror
(KEY_NAMES, SSH_CONFIG) = compileSshData()

def getProjectsVersions(projectList, branch = MIRRORS_BRANCH) {
    projectsVersions = [:]
    projectList.each { projectName ->
        projectRepo = getProjectRepoUrl(projectName)
        utils.retrier (NET_RETRY_COUNT) {
            revision = gerrit.getVersion(projectRepo, branch,
                                         INTERNAL_GERRIT_KEY)
            if (!revision) {
                error("Failed to get project version")
            }
        }
        projectsVersions[projectName] = revision
    }
    return projectsVersions
}


def getProjectsNotes(ucVersions, mirrorsVersions) {
    // get the list of new commits and relevant security notes
    utils.retrier (NET_RETRY_COUNT) {
        gerrit.cloneToBranch(
            getProjectRepoUrl('cicd-tools'),
            'master',
            'cicd-tools',
            INTERNAL_GERRIT_KEY,
            null
        )
    }
    projectNotes = [:]
    sh "sudo apt-get update; sudo apt-get install python-pip python-virtualenv -y"
    sh ("virtualenv venv; . venv/bin/activate; " +
    "pip install GitPython requests lxml")
    ucMirrorsVersions.each { repo_name, uc_revision ->
        revision = mirrorsVersions[repo_name]
        repo_dir = repo_name.split('/')[-1]
        utils.retrier (NET_RETRY_COUNT) {
            gerrit.cloneToBranch(
                getProjectRepoUrl(repo_name),
                MIRRORS_BRANCH,
                repo_dir,
                INTERNAL_GERRIT_KEY,
                null
            )
        }
        cmd = (". venv/bin/activate;"               +
               "python cicd-tools/secnotes.py "     +
               "--workdir . --project ${repo_dir} " +
               "--start-commit ${uc_revision} "     +
               "--end-commit ${revision}")
        secNotes = sh(returnStdout: true, script: cmd).trim()
        dir(repo_dir) {
            cmd = ("git log --oneline --no-merges ${uc_revision}...${revision}")
            changelog = sh(returnStdout: true, script: cmd).trim()
        }
        msg = ""
        if(secNotes) {
            msg += "Security fixes:\n${secNotes}\n"
        }
        if(changelog) {
            msg += "Changelog:\n${changelog}\n"
        }
        if(msg) {
            projectNotes[repo_dir] = msg
        }
    }
    return projectNotes
}


vm (initScript: 'loci-bootstrap.sh',
        image: 'cicd-ubuntu-16.04-server-cloudimg-amd64',
        flavor: 'm1.medium',
        nodePostfix: '',
        doNotDeleteNode: false) {
    // Create ssh config on slave to control what login is used for
    // what resource
    writeFile file: "${HOME}/.ssh/config", text: SSH_CONFIG
    sh "sudo bash -c 'echo \"nameserver ${DNS_SERVER_TWO}\" > /etc/resolv.conf'"
    stage("Determining mirrors updates") {
        env.GERRIT_TOPIC = TOPIC
        changes = gerrit.getTopicCommitInfo(
            REQ_PROJECT_NAME, INTERNAL_GERRIT_URL, INTERNAL_GERRIT_PORT,
            INTERNAL_GERRIT_KEY
        )

        if (changes.size() > 1) {
            error("Something wrong occurred. There should be either one or " +
                  "none open changes for topic ${TOPIC}.\n" +
                  "Please close extra changes.")
        }

        changeId = null
        if (changes) {
            changeId = changes[0]["id"]
        }
        utils.retrier (NET_RETRY_COUNT) {
            gerrit.cloneToBranch(
                getProjectRepoUrl(REQ_PROJECT_NAME),
                BRANCH,
                REQ_PROJECT_NAME,
                INTERNAL_GERRIT_KEY,
                null
            )
        }
        // Find all mirrors in upper_constraints.txt
        dir(REQ_PROJECT_NAME) {
            sh "git checkout ${BRANCH}"
            upperConstraints = readFile 'upper-constraints.txt'
        }
        ucMirrorsVersions = [:]
        (upperConstraints =~ /.*(${env.MIRRORS_PREFIX}.*?)@(.*)#.*/).each {
            ucMirrorsVersions[it[1]] = it[2]
        }
        mirrorsVersions = getProjectsVersions(ucMirrorsVersions.keySet(),
                                              MIRRORS_BRANCH)

        projectsNotes = getProjectsNotes(ucMirrorsVersions, mirrorsVersions)

        upperConstraintsUpdated = upperConstraints
        mirrorsVersions.each { mirror, version ->
            upperConstraintsUpdated = upperConstraintsUpdated.replaceAll(
                "(?<=git\\+(ssh|https)://.{0,30}/${mirror}@).*(?=#.*)",
                version)
        }
        if(upperConstraintsUpdated == upperConstraints) {
            print "Mirrors are up-to-date"
        } else {
            dir(REQ_PROJECT_NAME) {
                writeFile file: 'upper-constraints.txt', text: upperConstraintsUpdated
                sshagent([INTERNAL_GERRIT_KEY]) {
                    sh ("scp -p -P ${INTERNAL_GERRIT_PORT} " +
                        "${INTERNAL_GERRIT_URL}:hooks/commit-msg " +
                        ".git/hooks/")
                    msg = MESSAGE + "\n\n${env.BUILD_URL}\n"
                    projectNotes.each { project, notes ->
                        msg += "\n- ${project}:\n${notes}"
                    }
                    if (changeId) {
                        msg += "\nChange-Id: ${changeId}"
                    }
                    msg = msg.replace('"', "'")
                    sh "git ${COMMIT_ARGS} commit -a -m \"$msg\""
                    sh "git show HEAD"
                    sh "git push origin HEAD:refs/for/${BRANCH}/${TOPIC}"
                }
            }
        }
    }
}

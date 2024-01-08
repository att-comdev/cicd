import com.att.nccicd.config.conf as config
import groovy.json.JsonSlurperClassic

conf = new config(env).CONF
json = new JsonSlurperClassic()

LOCI_REPO = '${INTERNAL_GERRIT_SSH}/mirrors/opendev/loci'

LOCAL_WEB_PORT = '8080'
LOCAL_WEB_ADDR = '172.18.0.1'
NO_PROXY="${NO_PROXY},${LOCAL_WEB_ADDR}"
LOCAL_WEB_URL = "http://${LOCAL_WEB_ADDR}:${LOCAL_WEB_PORT}"
REPO_BASE_PATH = 'web/repo'
COMMIT_ARGS = ("-c 'user.name=Jenkins (${INTERNAL_GERRIT_USER})' " +
               "-c 'user.email=${INTERNAL_GERRIT_USER}@att.com'")
PY3 = 'no'

NET_RETRY_COUNT = env.NET_RETRY_COUNT.toInteger()

IMAGE = 'cicd-ubuntu-20.04-server-cloudimg-amd64'
if (['ussuri', 'victoria'].contains(RELEASE)) {
    IMAGE = 'cicd-ubuntu-18.04-server-cloudimg-amd64'
}
if (['antelope'].contains(RELEASE)) {
    IMAGE = 'cicd-ubuntu-22.04-server-cloudimg-amd64'
}

SEMANTIC_RELEASE_VERSION = "0.9.0"

def getOriginalCause(cause) {
    if (cause instanceof hudson.model.Cause$UpstreamCause) {
        cause = getOriginalCause(cause.getUpstreamCauses()[0])
    }
    return cause
}

def isStartedByUser() {
    cause = getOriginalCause(currentBuild.rawBuild.getCauses()[0])
    if (cause instanceof hudson.model.Cause$UserIdCause) {
        return true
    }
    return false
}

if (EVENT_TYPE != 'manual' && RESTRICT_EVENT_TYPE.toBoolean() &&
        isStartedByUser()) {
    error "Only 'manual' EVENT_TYPE is allowed for manual builds"
}

PS = ''

if (EVENT_TYPE != 'change-merged') {
    PS = '-patchset'
}

PROJECT_SUFFIX = env.PROJECT_NAME.split('-')[-1]
IMAGE_NAME = "mos-" + BUILD_TYPE

PY3 = 'affirmative'

RELEASE_BRANCH_MAP = json.parseText(RELEASE_BRANCH_MAP)
PROJECT_BRANCH = RELEASE_BRANCH_MAP[RELEASE]

PROJECT_REF = PROJECT_REF ? PROJECT_REF : PROJECT_BRANCH

DISPLAY_PREFIX = "${BUILD_TYPE} ${EVENT_TYPE} ${PROJECT_BRANCH}"

currentBuild.displayName = (
    "#${BUILD_NUMBER} ${DISPLAY_PREFIX}"
)

LOCI_BASE_IMAGE = conf.UBUNTU_BIONIC_BASE_IMAGE
OVS_REPO = conf.OVS_REPOS['bionic']
CEPH_REPO = conf.LOCI_CEPH_REPOS['bionic']
LIBVIRT_REPO = conf.LIBVIRT_REPOS['bionic']
if (['yoga', 'wallaby', 'xena'].contains(RELEASE)) {
    LOCI_BASE_IMAGE = conf.UBUNTU_FOCAL_BASE_IMAGE
    OVS_REPO = conf.OVS_REPOS['focal']
    CEPH_REPO = conf.LOCI_CEPH_REPOS['focal']
    LIBVIRT_REPO = conf.LIBVIRT_REPOS['focal']
}
if (['antelope'].contains(RELEASE)) {
    LOCI_BASE_IMAGE = conf.UBUNTU_JAMMY_BASE_IMAGE
    OVS_REPO = conf.OVS_REPOS['jammy']
    CEPH_REPO = conf.LOCI_CEPH_REPOS['jammy']
    LIBVIRT_REPO = conf.LIBVIRT_REPOS['jammy']
}

REQ_PROJECT_NAME = 'mos-requirements'
PROJECT_PREFIX = "loci/mos"
PROJECT_REPO = getProjectRepoUrl(PROJECT_NAME)

PROJECT_URL = ""
PROJECT_VERSION = ""

IMAGE_BASE = String.format(conf.MOS_IMAGES_BASE_URL, PS, RELEASE)

WHEELS_LATEST = (
    params.REQUIREMENTS_LOCI_IMAGE ?
    params.REQUIREMENTS_LOCI_IMAGE :
    "${String.format(conf.MOS_IMAGES_BASE_URL, '', RELEASE)}/mos-requirements:latest"
)

DEFAULT_ARGS = [
  'FROM':            "${LOCI_BASE_IMAGE}",
  'PROJECT':         "${PROJECT_SUFFIX}",
  'PROJECT_REF':     "${PROJECT_REF}",
  'PROJECT_RELEASE': "${RELEASE}",
// Commenting for future use of mirrored repositories
// after switching to git proto
//  'NOVNC_REPO':      "${getProjectRepoInternalUrl("novnc")}",
//  'SPICE_REPO':      "${getProjectRepoInternalUrl("spice-html5")}",
  'PROJECT_REPO':    getProjectRepoInternalUrl(PROJECT_NAME),
  'PYTHON3'     :    PY3,
  'KEEP_ALL_WHEELS': "True",
]

// keep proxy for nova and requirements as they multiple dependencies that need special
// handling (websockify with submodule, novnc, spice-htpm5 with shallow clonning)
// should be addressed separately switching to more sophisticated git proto
if (PROJECT_NAME =~ /nova|requirements|keystone/) {
    DEFAULT_ARGS << [
        'HTTP_PROXY':      "${HTTP_PROXY}",
        'HTTPS_PROXY':     "${HTTPS_PROXY}",
        'NO_PROXY':        "${NO_PROXY}",
        'http_proxy':      "${HTTP_PROXY}",
        'https_proxy':     "${HTTPS_PROXY}",
        'no_proxy':        "${NO_PROXY}",
    ]
}

SHELL = "sudo"

// Compile ssh-agent key names and ssh config from SSH_DATA to be used
// for fetching projects to internal mirror
(KEY_NAMES, SSH_CONFIG) = compileSshData()

def getProjectRepoUrl(prj) {
    return prj.contains("ssh://") ? prj : "ssh://${INTERNAL_GERRIT_URL}:${INTERNAL_GERRIT_PORT}/${prj}"
}

def getProjectRepoInternalUrl(prj) {
    return "${LOCAL_WEB_URL}/repo/${prj}"
}

def create_mirrors(projectList, keyList) {
    def limit = 10
    sshagent(keyList) {
        dir (REPO_BASE_PATH) {
            projectList.collate(limit).each { list ->
                def runningSet = [:]
                print "Clonning ${list}"
                list.each {
                    def url, name
                    (url, name) = [getProjectRepoUrl(it), it.split("/")[-1]]
                    runningSet[name] = {
                        utils.retrier(NET_RETRY_COUNT, { sleep 10 }) {
                            sh "git clone --mirror ${url} ${name}"
                        }
                        sh "git --git-dir ${name} update-server-info"
                    }
                }
                parallel runningSet
            }
        }
    }
}

def getRequirementsImageVersion() {
    pull(WHEELS_LATEST)
    cmd = (
        "${SHELL} docker inspect " +
        "--format='{{index .Config.Labels \"org.label-schema.vcs-ref\"}}' " +
        " ${WHEELS_LATEST}"
    )
    return sh(returnStdout: true, script: cmd).trim()
}

def getRevision(projectRepo, projectRef) {
    try {
        Long.parseLong(projectRef, 16)
        revision = projectRef
    } catch(java.lang.NumberFormatException ex) {
        revision = gerrit.getVersion(projectRepo, projectRef,
                                     INTERNAL_GERRIT_KEY)
    }
    if (!revision) {
        error ("Unable to determine revision " +
               "for ${projectRepo} from ${projectRef}")
    }
    return revision
}

def pushRequirements() {
    sshagent([INTERNAL_GERRIT_KEY]) {
        sh ("scp -p -P ${INTERNAL_GERRIT_PORT} " +
            "${INTERNAL_GERRIT_URL}:hooks/commit-msg " +
            "${REPO_BASE_PATH}/${REQ_PROJECT_NAME}/hooks/")
        gitArg = ("--git-dir ${REPO_BASE_PATH}/${REQ_PROJECT_NAME} " +
                  "--work-tree worktree")
        // pushing commit to repo creating Gerrit change first
        sh "git ${gitArg} ${COMMIT_ARGS} commit --amend -C HEAD"
        sh "git ${gitArg} config --unset remote.origin.mirror"
        sh "git ${gitArg} push origin HEAD:refs/for/${PROJECT_BRANCH}%topic=${UPDATE_TOPIC}"
        sh "git ${gitArg} push origin HEAD:${PROJECT_BRANCH}"
        sh "git ${gitArg} config --local remote.origin.mirror true"
    }
    PROJECT_VERSION = sh (script: "git ${gitArg} rev-parse HEAD",
                          returnStdout: true).trim()
    PROJECT_URL = getChangeUrl(REQ_PROJECT_NAME, PROJECT_VERSION)
}

def commit(gitArg, message) {
    sh "git ${gitArg} diff"
    sh("git ${gitArg} ${COMMIT_ARGS} commit -a -m \"${message}\"")
}

// Prepare local mirrors for project and it's dependencies specified in
// upper-constraints.txt of mos-requirements project. In case of
// mos-requirements image building, replaces external 'git+ssh://' urls in
// upper-constraints.txt to point local nginx.
// Another solution here could be to maintain separate list of projects to
// mirror, that would simplify current code, but having single source of
// truth is tempting
def prepare_local_mirrors = {
    // Determine if we need to create mirrors for non-requirements build
    // to avoid unnecessary clone of dozens dependency components
    def createDepMirrors = true
    if (!PROJECT_NAME.contains('requirements')) {
        loci.exportWheels("", WHEELS_LATEST)
        sh 'tar xf web/images/wheels.tar upper-constraints.txt'
        if (sh (returnStatus: true, script: "grep '^git+' upper-constraints.txt")) {
            createDepMirrors = false
        }
    }

    projectList = [PROJECT_NAME]
    // We need requirements project as upper-constraints.txt is
    // a single source of truth of a dependency list
    if (!PROJECT_NAME.contains('requirements') && createDepMirrors) {
        projectList.add(REQ_PROJECT_NAME)
    }
    sh "mkdir -p ${REPO_BASE_PATH}"
    create_mirrors(projectList, [INTERNAL_GERRIT_KEY])

    if (!createDepMirrors) { return }

    sh 'mkdir -p worktree'

    gitArg = ("--git-dir ${REPO_BASE_PATH}/${REQ_PROJECT_NAME} " +
              "--work-tree worktree")
    // Pick ref for requirements project from requirements image we build
    // current image with
    // PROJECT_REF is not provided for dependency review, default is used
    requirementsRef = (PROJECT_NAME.contains('requirements') ? PROJECT_REF :
                       getRequirementsImageVersion())
    sh "git ${gitArg} checkout ${requirementsRef}"
    // Creating a list of ssh dependency urls from upper-constraints.txt
    sshDependencyList = []
    uc_path = 'worktree/upper-constraints.txt'
    upperConstraints = readFile uc_path
    (upperConstraints =~ /git\+(ssh:\/\/.*?\/.*?)@.*/).each {
        sshDependencyList.add(it[1])
    }
    if (sshDependencyList) {
        create_mirrors(sshDependencyList, KEY_NAMES)
    }

    // for future switching to mirrored novnc and spice-html5
    //if (conf.LOCI_EXTRA_MIRRORS) {
    //    create_mirrors(conf.LOCI_EXTRA_MIRRORS, KEY_NAMES)
    //}

    // if we build requirements image, replace external urls to point nginx in
    // upper-constraints.txt and replace PROJECT_REF build argument
    // Also replace revisions for components in OVERRIDE_DEPENDENCY_LIST
    // if provided
    if (!PROJECT_NAME.contains('requirements')) { return }
    OVERRIDE_DEPENDENCY_LIST = env.DEPENDENCY_LIST.split()
    if (!(OVERRIDE_DEPENDENCY_LIST || sshDependencyList)) { return }

    sh "git ${gitArg} checkout -b local"
    DEFAULT_ARGS['PROJECT_REF'] = 'local'

    upperConstraintsUpdated = upperConstraints
    OVERRIDE_DEPENDENCY_LIST.each {
        (depName, depRef) = it.split(':')
        if (EVENT_TYPE == 'change-merged') {
            depRef = getRevision(getProjectRepoUrl(depName), depRef)
        }
        // Groovy does not support look-behind with undefined maximum length
        upperConstraintsUpdated = upperConstraintsUpdated.replaceAll(
            "(?<=git\\+(ssh|https)://.{0,30}/${depName}@).*(?=#.*)", depRef)
    }
    if (upperConstraintsUpdated != upperConstraints) {
        upperConstraints = upperConstraintsUpdated
        writeFile file: uc_path, text: upperConstraints
        commit(gitArg, "[update] Update dependency revisions")
        // push newly created commit to mos-requirements repository
        if (EVENT_TYPE == 'change-merged') {
            pushRequirements()
        }
    }

    upperConstraintsUpdated = upperConstraints.replaceAll(
        "((?<=git\\+)ssh://.*(?=/.+@))", "${LOCAL_WEB_URL}/repo")
    if (upperConstraintsUpdated != upperConstraints) {
        upperConstraints = upperConstraintsUpdated
        writeFile file: uc_path, text: upperConstraints
        commit(gitArg, "Switch to local urls")
    }

    sh "git ${gitArg} update-server-info"
}

def getChangeUrl(proj, projRevision) {
    cmd = "ssh -p ${INTERNAL_GERRIT_PORT} ${INTERNAL_GERRIT_URL} "
    cmd += "gerrit query --format=JSON commit:${projRevision} project:${proj}"
    sshagent([INTERNAL_GERRIT_KEY]) {
         queryResult = sh(returnStdout: true, script: cmd)
    }
    json.parseText(queryResult).get('url', "https://${INTERNAL_GERRIT_URL}/${PROJECT_NAME}")
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
    return json.parseText(SSH_DATA)
}

def pull(url) {
    utils.retrier (NET_RETRY_COUNT) {
        sh "${SHELL} docker pull ${url}"
    }
}

def push(imageTag, sha=null) {
    utils.retrier (NET_RETRY_COUNT) {
        result = sh (returnStdout: true,
                     script: "${SHELL} docker push ${imageTag}")
        print result
    }
    if (sha && !result.contains(sha)) {
        error("digest sha256 does not match")
    }
}

def buildLociMos(projConfArgs = null, wheelArgs = null) {
    pull(LOCI_BASE_IMAGE)
    def cmd = ("${SHELL} docker inspect --format='{{index .RepoDigests 0}}' " +
               "${LOCI_BASE_IMAGE}")
    def base_sha256 = sh(returnStdout: true, script: cmd).trim()

    def lociRef = conf.LOCI_REF

    LOCI_REPO_PATH = "/tmp/loci"
    // Clone loci repo to apply cicd patch for tests deletion
    def loci_version
    sshagent([INTERNAL_GERRIT_KEY]) {
        sh "git clone ${LOCI_REPO} ${LOCI_REPO_PATH}"
        dir (LOCI_REPO_PATH) {
            sh ("git fetch origin ${lociRef} && " +
                "git checkout FETCH_HEAD")
            loci_version = sh (script: "git rev-parse HEAD",
                               returnStdout: true).trim()
        }
    }

    // Copy script for tests cleanup to docker
    data = libraryResource "cicd/remove_tests.sh"
    writeFile file: 'remove_tests.sh', text: data

    sh "echo \"\$REMOVE_TESTS_FILE_DATA\" > " +
          "${LOCI_REPO_PATH}/scripts/remove_tests.sh"

    sh "chmod +x ${LOCI_REPO_PATH}/scripts/remove_tests.sh"

    // Add tests deletion step into install scenario for loci
    sh ("sed -i '/clone_project.sh/a \$(dirname \$0)/remove_tests.sh' " +
        "${LOCI_REPO_PATH}/scripts/install.sh")

    def labels = " --label org.label-schema.vcs-ref=${PROJECT_VERSION}\
      --label org.label-schema.build-date=${BUILD_TIMESTAMP}\
      --label org.label-schema.vcs-url=${PROJECT_URL}\
      --label org.label-schema.loci-ref=${loci_version}\
      --label org.label-schema.base-image=${base_sha256}\
      --label org.label-schema.vendor=\"${conf.CUSTOM_LABEL}\"\
      --label org.label-schema.version=${SEMANTIC_RELEASE_VERSION}.${BUILD_NUMBER}"

    if (!PROJECT_NAME.contains('requirements')) {
        cmd = ("${SHELL} docker inspect --format='{{index .RepoDigests 0}}' " +
               "${WHEELS_LATEST}")
        def requirements_sha256 = sh(returnStdout: true, script: cmd).trim()
        labels += (" --label org.label-schema.requirements-image=" +
                   "${requirements_sha256}")
    }

    if (EVENT_TYPE == 'change-merged') {
       labels += " --label org.label-schema.vcs-event=${EVENT_TYPE}"
    }

    customArgs = ["FROM": "base_image"]
    sh ("bash -c 'echo -e \"FROM ${LOCI_BASE_IMAGE}\n" +
        "ENV PIP_INDEX_URL=${ARTF_PIP_INDEX_URL}\"" +
        "| ${SHELL} docker build - -t ${customArgs.FROM}'")
    if (BUILD_TYPE =~ /neutron|nova/ && params.CUSTOM_OVS) {
        sh ("bash -c 'echo -e \"FROM ${customArgs.FROM}\n" +
            "RUN echo \"${OVS_REPO}\" >> /etc/apt/sources.list\"" +
            "| ${SHELL} docker build - -t ${customArgs.FROM}'")
    }
    if (CEPH_REPO && BUILD_TYPE =~ /nova|cinder|glance/) {
        sh ("bash -c 'echo -e \"FROM ${customArgs.FROM}\n" +
            "RUN echo \"${CEPH_REPO}\" >> /etc/apt/sources.list\"" +
            "| ${SHELL} docker build - -t ${customArgs.FROM}'")
    }
    if (LIBVIRT_REPO && BUILD_TYPE =~ /nova|cinder/) {
        sh ("bash -c 'echo -e \"FROM ${customArgs.FROM}\n" +
            "RUN echo \"${LIBVIRT_REPO}\" >> /etc/apt/sources.list\"" +
            "| ${SHELL} docker build - -t ${customArgs.FROM}'")
    }

    def argsMap = loci.mergeArgs([DEFAULT_ARGS, projConfArgs, wheelArgs, customArgs])
    def args = loci.buildParameters(argsMap)
    def image_tag = ("${IMAGE_BASE}/${IMAGE_NAME}:${PROJECT_VERSION}" +
                     ".${BUILD_TIMESTAMP}")
    ansiColor('xterm') {
        utils.retrier(NET_RETRY_COUNT) {
            sh ("${SHELL} docker build --force-rm --no-cache " +
                "${LOCI_REPO_PATH} ${args} ${labels} --tag ${image_tag}")
        }
    }
    push(image_tag)
    cmd = ("${SHELL} docker inspect " +
           "--format='{{index .RepoDigests 0}}' ${image_tag}")
    def image_sha = sh(returnStdout: true, script: cmd).trim()
    //publish latest (branch) tag on merge for requirements
    if (EVENT_TYPE == 'change-merged') {
        def image_latest = "${IMAGE_BASE}/${IMAGE_NAME}:latest"
        sh "${SHELL} docker tag ${image_tag} ${image_latest}"
        push(image_latest, image_sha.split('@')[-1])
    }

    return image_sha
}

vm (initScript: 'loci-bootstrap.sh',
    image: IMAGE,
    flavor: ('m1.' << (PROJECT_NAME.contains('requirements') ? 'large' : 'medium')),
    nodePostfix: '',
    doNotDeleteNode: false) {

    def cmd = ["echo \'DNS=${DNS_SERVER_TWO}\' >> /etc/systemd/resolved.conf",
               'systemctl daemon-reload',
               'systemctl restart systemd-networkd',
               'systemctl restart systemd-resolved',
               'systemctl stop docker.service ||:',
               'apt-get update && apt-get install -y runc containerd docker.io',
               // workaround to support existing requirements images
               // that has old address hard-coded
               "ip a a ${LOCAL_WEB_ADDR} dev docker0",
               'systemctl start docker.service ||:'].join('; ')
    sh "sudo bash -c \'${cmd}\'"

    stage('Init env') {
        loci.initEnv(ARTF_SECURE_DOCKER_URL, "jenkins-artifactory",
                     "", NET_RETRY_COUNT)
    }
    stage('Local Repo Setup') {
        loci.runNginx("", "${LOCAL_WEB_ADDR}:${LOCAL_WEB_PORT}", NET_RETRY_COUNT)
        // Create ssh config on slave to control what login is used for
        // what resource
        writeFile file: "${HOME}/.ssh/config", text: SSH_CONFIG
        PROJECT_VERSION = getRevision(PROJECT_REPO, PROJECT_REF)
        PROJECT_URL = getChangeUrl(PROJECT_NAME, PROJECT_VERSION)
        prepare_local_mirrors()
    }

    stage ("Build Project") {
        print "Building ${PROJECT_NAME.capitalize()}"
        if (PROJECT_NAME.contains('requirements')) {
            env.IMAGE_SHA = buildLociMos()
        } else {
            WHEELS_ARG = ['WHEELS': "${LOCAL_WEB_URL}/images/wheels.tar"]
            env.IMAGE_SHA = buildLociMos(
                loci.getDependencies(RELEASE, BUILD_TYPE),
                WHEELS_ARG
            )
        }
        env.PROJECT_VERSION = PROJECT_VERSION
        env.BUILD_TIMESTAMP = BUILD_TIMESTAMP
        env.LOCI_IMAGE_VAR = (
            "${BUILD_TYPE.replace('-', '_').toUpperCase()}_LOCI")
        sh "mkdir -p ${WORKSPACE}/artifacts"
        sh "echo $IMAGE_SHA | tee -a ${WORKSPACE}/artifacts/loci_image.txt"
        archiveArtifacts 'artifacts/*'
    }
}

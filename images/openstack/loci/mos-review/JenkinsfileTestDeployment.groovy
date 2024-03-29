import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

import com.att.nccicd.config.conf as config

conf = new config(env).CONF
json = new JsonSlurperClassic()

overrideImagesMap = json.parseText(OVERRIDE_IMAGES)

DISTRO_VERSION = 'bionic'

RELEASE_BRANCH_MAP = json.parseText(RELEASE_BRANCH_MAP)
BRANCH = RELEASE_BRANCH_MAP[RELEASE]

def getProjectRepoUrl(prj) {
    return prj.contains("ssh://") ? prj : "${INTERNAL_GERRIT_SSH}/${prj}"
}

NET_RETRY_COUNT = NET_RETRY_COUNT.toInteger()
MANIFESTS_BRANCH = conf.OS_RELEASE_MANIFESTS_BRANCH_MAP[RELEASE]
MANIFESTS_PROJECT_NAME = conf.GLOBAL_REPO
VERSIONS_PATH = conf.VERSIONS_PATH
IMAGE_BASE_URL = String.format(conf.MOS_IMAGES_BASE_URL, "", RELEASE)
RELEASES_REGEX = "(${json.parseText(env.SUPPORTED_RELEASES).join("|")})"
RELEASE_OVERRIDES = conf.OSH_AIO_RELEASE_OVERRIDES
REPOS = conf.OSH_AIO_REPOS_BIONIC
BUILD_KUBEADM = true
SNAPSHOT_NAME = "osh-aio-${RELEASE}-initial"
OLD_SNAPSHOT_ID = ""
INITIAL_DEPLOYMENT = INITIAL_DEPLOYMENT.toBoolean()
CREATE_SNAPSHOT = INITIAL_DEPLOYMENT && CREATE_SNAPSHOT.toBoolean()
BASE_IMAGE = 'cicd-ubuntu-18.04-server-cloudimg-amd64'
TROUBLESHOOTING = TROUBLESHOOTING.toBoolean()
AVAILABLE_IMAGES = []
DEFAULT_IMAGES = [:]
EXTRA_CHARTS = ['Barbican']

if (TROUBLESHOOTING) {
    assert !INITIAL_DEPLOYMENT
}

def cloneOSH() {
    sh 'mkdir -p $WORKSPACE/artifacts'

    for (proj in ['openstack-helm', 'openstack-helm-infra']) {
        git_url = "${INTERNAL_GERRIT_SSH}/mirrors/opendev/${proj}.git"
        branch = "master"
        withEnv(["SHALLOW_CLONE=false"]) {
            gerrit.cloneProject(git_url, branch, "", "${WORKSPACE}/${proj}", INTERNAL_GERRIT_KEY)
        }
        version = gerrit.getVersion(git_url, branch, INTERNAL_GERRIT_KEY)
        sh "echo ${proj} head is at ${version} | tee -a ${WORKSPACE}/artifacts/OSH_version.txt"
    }
}


def imageOverrides(Map images, Map debugOverrides=[:]) {
    imageTypes = ['nova', 'nova-1804', 'neutron', 'neutron-sriov', 'glance',
                  'cinder', 'heat', 'horizon', 'keystone', 'placement',
                  'barbican']
    // populate images with default values
    imageTypes.each {
        key = "${it.replace('-', '_').toUpperCase()}_LOCI"
        if (images[key] == null) {
            images[key] = "${IMAGE_BASE_URL}/mos-${it}:latest"
        }
    }
    // replace upstream docker registries to artifactory cache
    ['docker\\.io', 'quay\\.io', 'k8s\\.gcr\\.io', 'gcr\\.io'].each {
        sh ("find . -type f -exec sed -i 's#${it}#${ARTF_DOCKER_URL}#g' {} +")
    }
    sh ("find . -type f -exec sed -i 's# \\(calico/ctl\\)# ${ARTF_DOCKER_URL}/\\1#g' {} +")

    utils.retrier (NET_RETRY_COUNT) {
        gerrit.cloneToBranch(
            getProjectRepoUrl(MANIFESTS_PROJECT_NAME),
            MANIFESTS_BRANCH,
            MANIFESTS_PROJECT_NAME,
            INTERNAL_GERRIT_KEY,
            MANIFESTS_BRANCH
        )
    }
    dir(MANIFESTS_PROJECT_NAME) {
        versions = readFile VERSIONS_PATH
        images.each { _, image ->
            (_, replace_to, pattern) = ((image =~ /.*?\/((.*?)[@:].*)/)[0])
            // For pattern replace actual release name by regex matching any release
            pattern = pattern.replaceAll(RELEASES_REGEX, RELEASES_REGEX) << '[@:].*'
            pattern = pattern.replaceAll('openstack-patchset', 'openstack') << '[@:].*'
            versions = versions.replaceAll(pattern, replace_to)
        }
        writeFile file: VERSIONS_PATH, text: versions
        sh "git diff"
        sh ("sed -i 's#DOCKER_OPEN_DOMAIN#${ARTF_DOCKER_URL}#g' ${VERSIONS_PATH}")
        sh ("sed -i 's#DOCKER_DOMAIN#${ARTF_SECURE_DOCKER_URL}#g' ${VERSIONS_PATH}")
        versionsData = (readYaml(file: VERSIONS_PATH))['data']['images']
    }

    def overrideYaml
    def releaseOverrides
    ['osh', 'ceph'].each {
        versionsData[it].each { chart, overrides ->
            if (fileExists("openstack-helm/${chart}")) {
                chartDir = "openstack-helm"
            } else if (fileExists("openstack-helm-infra/${chart}")) {
                chartDir = "openstack-helm-infra"
            } else { return }
            releaseOverrides = RELEASE_OVERRIDES[RELEASE][chart]
            if (releaseOverrides) {
                overrides << releaseOverrides
            }
            chartDebugOverrides = debugOverrides[chart]
            if (chartDebugOverrides) {
                overrides << chartDebugOverrides
            }
            dir (chartDir) {
                overrideYaml = "${chart}/values_overrides/${RELEASE}-ubuntu_${DISTRO_VERSION}.yaml"
                def overrideData = (fileExists(overrideYaml) ?
                    readYaml(file: overrideYaml) : [:])
                overrideData['images'] = ["tags": overrides]
                sh "rm -rf ${overrideYaml}"
                writeYaml file: overrideYaml, data: overrideData
            }
        }
    }
    // update cirros image location to internal mirror to allow access from rally without proxy.
    // get glance test schema error while defining OSH_EXTRA_HELM_ARGS_GLANCE with --set overrides
    // replacing the url string for now
    sh """sed -i -e "s|http://download.cirros-cloud.net/0.3.5/|${conf.CIRROS_IMAGE_PATH}|" \\
          ${WORKSPACE}/openstack-helm/glance/values.yaml"""
    sh """sed -i -e "s|image_location: .*cirros-.*img|image_location: ${conf.CIRROS_IMAGE_PATH}cirros-0.3.5-x86_64-disk.img|" \\
          ${WORKSPACE}/openstack-helm/glance/values.yaml"""
}


def installPackages() {
    packages = 'apt-transport-https ca-certificates curl software-properties-common python3-dev python3-pip libffi-dev'
    sh "sudo apt-get update && sudo apt-get install -y ${packages}"
    REPOS.each { component, data ->
        sh "sudo bash -c 'echo \"${data.source}\" >> /etc/apt/sources.list.d/${component}.list'"
        sh "sudo bash -c 'echo \"${data.pref}\" >> /etc/apt/preferences.d/${component}.pref'"
    }
    sh 'sudo apt-get update && sudo apt-get install -y docker-ce ceph-common rbd-nbd'
    sh "sudo systemctl daemon-reload"
    sh "sudo systemctl restart docker"
}


def tweakOSH() {
    conf.OSH_AIO_PINNINGS[RELEASE].each { repo, ref ->
        dir (repo) {
            sh "git checkout ${ref}"
        }
    }
    dir ("openstack-helm") {
        sh 'git config user.email "T-850@model.101"'
        sh 'git config user.name "T-850 Model 101"'
        [].each {
            sh ("git fetch https://review.opendev.org/openstack/openstack-helm refs/changes/${it} && git cherry-pick FETCH_HEAD")
        }
        // Pin get-values-overrides.sh as it's altered to fix upstream gates
        sh "bash -c 'git checkout 97ac0575ba1127a2a16a35fba9a57ddeda1acc26 -- tools/deployment/common/{get-values-overrides,env-variables}.sh'"
    }

    dir ("openstack-helm-infra") {
        sh 'git config user.email "T-850@model.101"'
        sh 'git config user.name "T-850 Model 101"'
        sh "bash -c 'git revert --no-edit b2adfeadd8adbf5d99187106cf5d2956f0afeeab | true'"
    }
    if (TROUBLESHOOTING) {
        // add tty: true and stding true to each service pod template
        dir ('openstack-helm') {
            sh (returnStdout: true, script: 'find . \\( -iname "deployment*.yaml" -o -iname "daemonset*.yaml" \\)').split("\n").each {
                fname = it.trim()
                text = readFile fname
                try {
                    (pattern, indent) = ((text =~ / *containers:\n( *- ).*/)[0])
                } catch(Exception ex) {
                    println("Skipping ${fname}")
                    return
                }
                indent = " " * indent.length()
                text = text.replaceAll(pattern, pattern << "\n${indent}tty: true\n${indent}stdin: true")
                writeFile file: fname, text: text
            }
        }
    }

    sh 'sed -i "/pyopenssl/a \\ \\ sudo -H -E pip3 install --upgrade alembic==1.4.3" ./openstack-helm-infra/tools/gate/devel/start.sh'
    // until https://review.opendev.org/#/c/738141/ is merged
    sh 'sed -i \'/OSH_EXTRA_HELM_ARGS_NOVA/a export OSH_EXTRA_HELM_ARGS_NOVA=\"${OSH_EXTRA_HELM_ARGS_NOVA} $(./tools/deployment/common/get-values-overrides.sh nova)\"\' ./openstack-helm/tools/deployment/developer/ceph/160-compute-kit.sh'
    // This is the most dirty part to make OSH AIO to use customizied artifacts location for k8s
    // Apparenly avialability of configuration overriding should be addressed upstream
    sh '''sed -i "/external_dns_nameservers:/a\\      - ${DNS_SERVER_TWO}\\n      - ${DNS_SERVER_ONE}" \
          ./openstack-helm-infra/tools/images/kubeadm-aio/assets/opt/playbooks/vars.yaml'''
    def defaults = './openstack-helm-infra/roles/build-images/defaults/main.yml'
    def amap = readYaml file: defaults
    amap['url'] = [
        'google_kubernetes_repo': "${ARTIFACTS_URL}/kubernetes-release/release/{{ version.kubernetes }}/bin/linux/amd64",
        'google_helm_repo': "${ARTIFACTS_URL}/helm-binaries",
        'helm_repo': "${ARTIFACTS_URL}/helm-binaries",
        'cni_repo': "${ARTIFACTS_URL}/containernetworking/download/{{ version.cni }}",
    ]
    sh "rm -rf ${defaults}"
    writeYaml file: defaults, data: amap

    defaults = './openstack-helm-infra/roles/build-helm-packages/defaults/main.yml'
    amap = readYaml file: defaults
    amap['url'] = [
        'google_helm_repo': "${ARTIFACTS_URL}/helm-binaries",
        'helm_repo': "${ARTIFACTS_URL}/helm-binaries",
    ]
    sh "rm -rf ${defaults}"
    writeYaml file: defaults, data: amap

    def kubeadmDockerfileName = "./openstack-helm-infra/tools/images/kubeadm-aio/Dockerfile"
    def kubeadmDockerfile = readFile kubeadmDockerfileName
    kubeadmDockerfile = kubeadmDockerfile.replaceAll(
        'FROM docker.io/ubuntu:.*', "FROM ${conf.UBUNTU_BIONIC_BASE_IMAGE}").replaceAll(
        'ENV PIP_INDEX_URL.*', "ENV PIP_INDEX_URL=${ARTF_PIP_INDEX_URL}").replaceAll(
        '.*PIP_TRUSTED_HOST.*', '').replaceAll(
        'ARG UBUNTU_URL=.*', "ARG UBUNTU_URL=${ARTF_UBUNTU_REPO}/")
    writeFile file: kubeadmDockerfileName, text: kubeadmDockerfile

    // we cover this part from internal ceph repo
    sh "sed -i '/support-packages/d' ./openstack-helm-infra/tools/images/kubeadm-aio/assets/opt/playbooks/roles/deploy-kubelet/tasks/main.yaml"
    // override kubelet default pod infra container image to use an internal one
    sh "sed -i 's#KUBELET_SYSTEM_PODS_ARGS=#\\0--pod-infra-container-image ${ARTF_DOCKER_URL}/pause:3.1 #g' ./openstack-helm-infra/tools/images/kubeadm-aio/assets/opt/playbooks/roles/deploy-kubelet/templates/10-kubeadm.conf.j2"
    sh "sed -i 's#https://pkg.cfssl.org#${ARTIFACTS_URL}/cfssl#g' ./openstack-helm-infra/tools/images/kubeadm-aio/assets/opt/playbooks/roles/deploy-kubeadm-master/tasks/helm-cni.yaml"
}


def installOpenstackClient() {
    sshagent ([INTERNAL_GERRIT_KEY]) {
        sh "git clone -b ${BRANCH} ${INTERNAL_GERRIT_SSH}/mos-requirements"
        dir ("mos-requirements") {
            def upperConstraints = readFile "upper-constraints.txt"
            upperConstraints = upperConstraints.replaceAll(
                "ssh://", "ssh://${INTERNAL_GERRIT_USER}@"
            )
            writeFile file: "upper-constraints.txt", text: upperConstraints
        }
        pip = 'pip3'
        if (!sh (returnStatus: true, script: "sudo ${pip} uninstall python-openstackclient -y")) {
            dir ("openstack-helm") {
                sh "sudo ${pip} install pip===20.3.4 -U"
                withEnv (["UPPER_CONSTRAINTS_FILE=${WORKSPACE}/mos-requirements/upper-constraints.txt", "PIP_USE_DEPRECATED=legacy-resolver"]) {
                    sh ("./tools/deployment/developer/ceph/020-setup-client.sh")
                }
            }
        }
    }
}


def TestVm(Map map, Closure body) {

    // Startup script to run after VM instance creation
    //  bootstrap.sh - default
    //  loci-bootstrap.sh - for loci builds
    def initScript = map.initScript ?: 'bootstrap.sh'

    // image used for creating instance
    def image = map.image ?: 'cicd-ubuntu-16.04-server-cloudimg-amd64'

    // flavor type used for creating instance
    def flavor = map.flavor ?: 'm1.medium'

    // postfix string for instance nodename
    def nodePostfix = map.nodePostfix ?: ''

    // build template used for heat stack creation
    //  basic - default
    //  loci - for loci builds
    def buildType = map.buildType ?: 'basic'

    // Flag to control node cleanup after job execution
    // Useful for retaining env for debugging failures
    // NodeCleanup job be used to destroy the node later
    //  false - default, deletes node after job
    //  true - do not delete node
    def doNotDeleteNode = map.doNotDeleteNode ?: false

    // Flag to control Jenkins console log publishing to Artifactory.
    //
    // This will also set custom URL to be returned when voting in Gerrit
    // https://jenkins.io/doc/pipeline/steps/gerrit-trigger/
    //
    // Useful for providing Jenkins console log when acting as 3rd party gate,
    // especially when Jenkins itself is not accessible
    def artifactoryLogs = map.artifactoryLogs ?: false

    // global timeout for executing pipeline
    // useful to prevent forever hanging pipelines consuming resources
    def globalTimeout = map.timeout ?: 180

    // if useJumphost is true floating ip won't be assigned to vm.
    // Jenkins will access vm via jumphost configured in global configuration
    // with OS_JUMPHOST_PUBLIC_IP variable
    def useJumphost = map.useJumphost
    if (useJumphost == null) {
        useJumphost = env.OS_JUMPHOST_PUBLIC_IP ? true : false
    }

    // Name of public network that is used to allocate floating IPs
    def publicNet = useJumphost ? '' : (map.publicNet ?:
                                        env.OS_PUBLIC_NET ?:
                                        'public')

    // Name of private network for the VM
    def privateNet = map.privateNet ?: 'private'

    // resolve args to heat parameters
    def parameters = " --parameter image=${image}" +
                     " --parameter flavor=${flavor}" +
                     " --parameter public_net=${publicNet}" +
                     " --parameter private_net=${privateNet}"

    // node used for launching VMs
    def launch_node = 'jenkins-node-launch'

    def name = map.label ?: "${JOB_BASE_NAME}-${BUILD_NUMBER}"

    def postBuildSuccessBody = map.postBuildSuccessBody ?: {}
    def postBuildFailureBody = map.postBuildFailureBody ?: {}
    def preBuildBody = map.preBuildBody ?: null
    def troubleshootingBody = map.troubleshootingBody ?: null

    // templates located in resources from shared libraries
    // https://github.com/att-comdev/cicd/tree/master/resources
    def stack_template="heat/stack/ubuntu.${buildType}.stack.template.yaml"

    // optionally uer may supply additional identified for the VM
    // this makes it easier to find it in OpenStack (e.g. name)
    if (nodePostfix && !label) {
      name += "-${nodePostfix}"
    }

    def ip = ""
    def port = "22"

    timestamps {
        try {
            utils.retrier(
                3,
                {
                    node('master') {
                        jenkins.node_delete(name)
                    }
                    node(launch_node) {
                        heat.stack_delete(name)
                    }
                }
            ) {
                stage ('Node Launch') {

                    node(launch_node) {
                        tmpl = libraryResource "${stack_template}"
                        writeFile file: 'template.yaml', text: tmpl

                        if (initScript) {
                            data = libraryResource "heat/stack/${initScript}"
                            writeFile file: 'cloud-config', text: data
                        }

                        utils.retrier(3, { heat.stack_delete(name) }) {
                            heat.stack_create(
                                name,
                                "${WORKSPACE}/template.yaml",
                                parameters
                            )
                        }
                        ip = heat.stack_output(name, 'routable_ip')
                        if (useJumphost) {
                            port = (ip.split('\\.')[-1].toInteger() + 10000).toString()
                            ip = OS_JUMPHOST_PUBLIC_IP
                        }
                    }

                    node('master') {
                        utils.retrier(3, { jenkins.node_delete(name) }) {
                            jenkins.node_create (name, ip, port)
                        }

                        timeout (14) {
                            node(name) {
                                sh 'cloud-init status --wait'
                            }
                        }
                    }
                }
            }
            if (preBuildBody) {
                node (name) {
                    preBuildBody()
                }
                sleep 60
            }
            // execute pipeline body, everything within vm()
            node (name) {
                try {
                    vm.message ('READY: JENKINS WORKER LAUNCHED') {
                        print "Launch overrides: ${map}\n" +
                              "Pipeline timeout: ${globalTimeout}\n" +
                              "Heat template: ${stack_template}\n" +
                              "Node IP: ${ip}:${port}"
                    }
                    timeout(globalTimeout) {
                        vm.setKnownHosts()
                        body()
                    }
                    vm.message ('SUCCESS: PIPELINE EXECUTION FINISHED') {}
                    currentBuild.result = 'SUCCESS'

                // use Throwable to catch java.lang.NoSuchMethodError error
                } catch (Throwable err) {
                    vm.message ('FAILURE: PIPELINE EXECUTION HALTED') {
                        print "Pipeline body failed or timed out: ${err}.\n" +
                              'Likely gate reports failure.\n'
                    }
                    currentBuild.result = 'FAILURE'
                    throw err
                }
            }
            if (troubleshootingBody) {
                troubleshootingBody(name)
            }
            node(launch_node) {
                postBuildSuccessBody(name)
            }
        // use Throwable to catch java.lang.NoSuchMethodError error
        } catch (Throwable err) {
            node(launch_node) {
                postBuildFailureBody()
            }
            vm.message ('ERROR: FAILED TO LAUNCH JENKINS WORKER') {
                print 'Failed to launch Jenkins VM/worker.\n' +
                      'Likely infra/template or config error.\n' +
                      "Error message: ${err}"
            }
            currentBuild.result = 'FAILURE'
            throw err

        } finally {
            if (!doNotDeleteNode) {
                node('master') {
                    jenkins.node_delete(name)
                }
                node(launch_node) {
                   heat.stack_delete(name)
                }
            }
        }
        return ip
    }
}

def installOSHAIO(List steps, concurrent=true) {
    // see https://docs.openstack.org/openstack-helm/latest/install/developer/index.html
    def deploy_steps = ['Packages'   : 'common/000-install-packages.sh',
                        'Kubernetes' : 'common/010-deploy-k8s.sh',
                        'Clients'    : 'common/020-setup-client.sh',
                        'Ingress'    : 'common/030-ingress.sh',
                        'Ceph'       : 'ceph/040-ceph.sh',
                        'Ceph NS'    : 'ceph/045-ceph-ns-activate.sh',
                        'MariaDB'    : 'ceph/050-mariadb.sh',
                        'RabbitMQ'   : 'ceph/060-rabbitmq.sh',
                        'Memcached'  : 'ceph/070-memcached.sh',
                        'Keystone'   : 'ceph/080-keystone.sh',
                        'Barbican'   : 'common/085-barbican.sh',
                        'Heat'       : 'ceph/090-heat.sh',
                        'Horizon'    : 'ceph/100-horizon.sh',
                        'Rados GW'   : 'ceph/110-ceph-radosgateway.sh',
                        'Glance'     : 'ceph/120-glance.sh',
                        'Cinder'     : 'ceph/130-cinder.sh',
                        'Openvswitch': 'ceph/140-openvswitch.sh',
                        'Libvirt'    : 'ceph/150-libvirt.sh',
                        'Compute Kit': 'ceph/160-compute-kit.sh',
                        'Gateway'    : 'ceph/170-setup-gateway.sh']

    deploymentEnv = [
        'OS_REGION_NAME=',
        'OS_USERNAME=',
        'OS_PASSWORD=',
        'OS_PROJECT_NAME=',
        'OS_PROJECT_DOMAIN_NAME=',
        'OS_USER_DOMAIN_NAME=',
        'OS_AUTH_URL=',
        'PIP_USE_DEPRECATED=legacy-resolver',
        "OPENSTACK_RELEASE=${RELEASE}",
        "OSH_OPENSTACK_RELEASE=${RELEASE}",
        "CONTAINER_DISTRO_VERSION=${DISTRO_VERSION}",
        "UPPER_CONSTRAINTS_FILE=${WORKSPACE}/mos-requirements/upper-constraints.txt",
    ]
    runningSet = [:]
    def i = 0
    steps.each { it ->
        def y = i
        runningSet[it] = {
            withEnv(deploymentEnv) {
                if (concurrent) {
                    sleep 5*y
                }
                print "Installing ${it}..."
                dir ('openstack-helm') {
                    sh "./tools/deployment/developer/${deploy_steps[it]}"
                }
            }
        }
        i ++
    }
    if (concurrent) {
        parallel runningSet
    } else {
        runningSet.each { _, closure -> closure() }
    }
}

def openstackExec(cmd) {
    withCredentials([usernamePassword(credentialsId: 'jenkins-openstack-18',
                                      usernameVariable: 'OS_USERNAME',
                                      passwordVariable: 'OS_PASSWORD')]) {
        sh (returnStdout: true, script: heat.openstack_cmd(cmd)).trim()
    }
}

def createSnapshot = { stackName ->
    sleep 60
    serverId = openstackExec(
        "openstack stack resource show ${stackName} server " +
        "-c physical_resource_id -f value"
    )
    try {
        OLD_SNAPSHOT_ID = openstackExec(
            "openstack image show -f value -c id ${SNAPSHOT_NAME}")
    } catch (Exception) {
        print "${SNAPSHOT_NAME} not found"
    }
    openstackExec("openstack server image create ${serverId} " +
                  "--name ${SNAPSHOT_NAME}-tmp --wait")
}

def replaceImage = {
    openstackExec(
        "openstack image set ${SNAPSHOT_NAME}-tmp --name ${SNAPSHOT_NAME}")
    if (OLD_SNAPSHOT_ID) {
        deleteImage(OLD_SNAPSHOT_ID)
    }
}

def deleteImage(image) {
    openstackExec("openstack image delete ${image}")
}

def setupDNSConfiguration() {
    sh "sudo bash -c 'echo \"nameserver ${DNS_SERVER_TWO}\" > /etc/resolv.conf'"
}

def deleteInitialSnapshot = {
    deleteImage(SNAPSHOT_NAME << "-tmp")
}


def runHelmTests(tests, run_from_root=false) {
    _sudo = ""
    if (run_from_root) {
        _sudo = "sudo"
    }
    sh 'mkdir -p $WORKSPACE/artifacts'
    def results = []
    def runningSet = [:]

    tests.each { it ->
        runningSet[it] = {
            def res = ""
            def secondRetry = false
            try {
                utils.retrier(2) {
                    try {
                        cmd = "${_sudo} helm test --debug ${it} --timeout 900 2>\\&1"
                        result = sh (returnStdout: true, script: cmd)
                    } catch (Exception e) {
                        if (!secondRetry) {
                            sh "bash -c 'kubectl delete pod ${it}-test -nopenstack ||:'"
                            secondRetry = true
                        }
                        throw(e)
                    }
                }
                res += result
            } catch (Exception e) {
                cmd = "${_sudo} kubectl logs ${it}-test --namespace openstack 2>\\&1"
                res += sh (returnStdout: true, script: cmd)
                throw e
            } finally {
                results.add(res)
            }
        }
    }
    try {
        parallel runningSet
    } finally {
        writeFile file: "${WORKSPACE}/artifacts/helm_tests.log", text: results.join("\n")
    }
}


def exportPipEnv = {
    sh "sudo bash -c 'echo PIP_INDEX_URL=${ARTF_PIP_INDEX_URL} >> /etc/environment'"
}


def updateHost = {
    def cmd = ['export DEBIAN_FRONTEND=noninteractive',
               'export apt_opts="-o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold"',
               'apt-get update',
               'apt-get remove -y runc containerd docker.io',
               'apt-get \${apt_opts} upgrade -y',
               'apt-get \${apt_opts} dist-upgrade -y',
               'apt autoremove -y',
               'echo DefaultLimitMEMLOCK=16386 >> /etc/systemd/system.conf',
               'systemctl daemon-reexec'].join('; ')

    sh "sudo bash -c \'${cmd}\'"
}


def setupHugepages = {
    grubPgs = "grub-common grub-legacy-ec2 grub-pc grub-pc-bin grub2-common"
    sh "sudo apt-get update && sudo apt-get install ${grubPgs} -y"
    data = readFile "/etc/default/grub"
    data = data.replaceAll(
        "GRUB_CMDLINE_LINUX_DEFAULT=.*",
        "GRUB_CMDLINE_LINUX_DEFAULT=\"console=tty1 console=ttyS0 " +
        "default_hugepagesz=1G hugepagesz=1G hugepages=2 intel_iommu=on iommu=pt\""
    ).replaceAll(
        "GRUB_CMDLINE_LINUX=.*",
        "GRUB_CMDLINE_LINUX=\"console=tty1 console=ttyS0 default_hugepagesz=1G " +
        "hugepagesz=1G hugepages=2 intel_iommu=on iommu=pt\""
    )
    writeFile file: "${HOME}/grub", text: data
    sh "sudo mv ${HOME}/grub /etc/default/"
    sh 'sudo bash -c "update-grub"'
    sh 'sudo bash -c "echo \'none /mnt/huge hugetlbfs pagesize=1G,size=2G 0 0\' >> /etc/fstab"'
    sh 'sudo bash -c "echo \'sleep 1; sudo reboot;\' | at now"'
}


def setupNode = {
    vm.setKnownHosts()
    if (env.VM_PRE_HOOK_CMD) {
        sh VM_PRE_HOOK_CMD
    }
    updateHost()
    sh "sudo cp ${HOME}/.ssh/known_hosts /etc/ssh/ssh_known_hosts"
    exportPipEnv()
    setupHugepages()
}


K8S_DEPLOY_STEPS = [
    {
        stage('Setup host') {
            setupDNSConfiguration()
        }
    },
    {
        stage('Install packages') {
            installPackages()
        }
    },
    {
        stage('Authenticate docker repo') {
            utils.retrier(NET_RETRY_COUNT) {
                osh.dockerAuth(ARTF_READONLY_CREDS)
            }
        }
    },
    {
        stage('Clone OpenstackHelm') {
            cloneOSH()
            tweakOSH()
        }
    },
    {
        stage('Install OpenstackClient') {
            installOpenstackClient()
        }
    },
    {
        stage('Override images') {
            // Override default OSH images from global manifests, RELEASE_OVERRIDES,
            // latest mos set and OVERRIDE_IMAGES map and creates override yamls
            // for every component.
            // Also replaces all mentions of upstream registries to artifactory cache
            imageOverrides(overrideImagesMap)
        }
    },
    {
        stage('Download precreated kubeadm-aio image') {
            if (!BUILD_KUBEADM) {
                // Pulls kubeadm image and disables it's build saving ~1h
                utils.retrier(NET_RETRY_COUNT) {
                    sh "sudo docker pull ${conf.OSH_AIO_KUBEADM_IMAGE}"
                }
                sh "sudo docker tag ${conf.OSH_AIO_KUBEADM_IMAGE} openstackhelm/kubeadm-aio:dev"
                sh "sudo docker rmi ${conf.OSH_AIO_KUBEADM_IMAGE}"
                sh "echo '' > openstack-helm-infra/roles/build-images/tasks/kubeadm-aio.yaml"
            }
        }
    },
    {
        stage('Install k8s cluster') {
            try {
                installOSHAIO(['Packages', 'Kubernetes'], concurrent=false)
                sshagent ([INTERNAL_GERRIT_KEY]) {
                    installOSHAIO(['Clients'], concurrent=false)
                }
                installOSHAIO(['Ingress', 'Ceph', 'Ceph NS'], concurrent=false)
            } catch (Exception e) {
                artifactLogs()
                error "k8s deployment failed with exception ${e}"
            }
        }
    },
]


CMDS = [
    'reboot': 'sudo bash -c "echo \'sleep 5; sudo reboot;\' | at now"'
]


def readConfig(nodeName) {
    def deployedRevision = ""
    def data
    def deathTime = null
    def cmd = null
    def finish = null
    while (true) {
        if (finish) {
            break
        }
        sleep 60
        node (nodeName) {
            while (fileExists("config/lock")) { sleep 5; }
            sh "touch config/lock"
            if (!deployedRevision) {
                dir ("config") {
                    deployedRevision = sh (
                        returnStdout: true,
                        script: "git rev-list --max-parents=0 HEAD"
                    ).trim()
                }
            }

            newDeathTime = new String(
                (readFile("config/deathTime")).trim().decodeBase64())
            newDeathTime = Long.valueOf(newDeathTime)
            if (deathTime != newDeathTime) {
                print ("New death time detected: " <<
                       new Date(newDeathTime).format("yyyy-MM-dd HH:mm:ss 'UTC'"))
                deathTime = newDeathTime
            }
            if (deathTime < System.currentTimeMillis()) {
                finish = true
                return
            }

            if (fileExists("config/apply")) {
                requestedRevision = (readFile("config/apply")).trim()
                if (fileExists("config/deployedRevision")) {
                    deployedRevision = (
                        readFile("config/deployedRevision")).trim()
                }
                sh "rm config/apply"
                def failed = false
                dir ("config") {
                    sh "git checkout ${requestedRevision} -- config"
                    data = json.parseText(
                        new String((readFile("config")).trim().decodeBase64()))
                    sh "git reset --hard HEAD"
                }
                newConfig = data.config ?: [:]
                try {
                    ["keystone", "heat", "horizon", "glance",
                     "cinder", "neutron", "nova"].each { chart ->
                        def tags = newConfig[chart] ?: [:]
                        print "Applying configuration for ${chart}:\n${tags}"
                        apply(chart, tags, requestedRevision)
                    }
                } catch (Exception e) {
                    failed = true
                    print "Failed to apply ${requestedRevision}"
                    writeFile (file: "config/failedRevision",
                               text: requestedRevision)
                    print "Initiating rollback to ${deployedRevision}"
                    writeFile (file: "config/apply", text: deployedRevision)
                }
                if (!failed) {
                    deployedRevision = requestedRevision
                    writeFile (file: "config/deployedRevision",
                               text: deployedRevision)
                }
            }
            sh "rm config/lock"

            if (fileExists("reboot")) {
                sh "rm reboot"
                command = CMDS['reboot']
                sh command
            }
        }
    }
}


def apply(chart, tags, configRevision) {
    dir ("openstack-helm") {
        oshDiff = sh (script: "git diff", returnStdout: true).trim()
        if (oshDiff) {
            sh "git add -A && git commit -m 'Pre-update ${configRevision} commit'"
        }
    }
    prepareImages(chart, tags)
    dir ("openstack-helm") {
        oshDiff = sh (script: "git diff", returnStdout: true).trim()
        if (!oshDiff) {
            print "Nothing to apply for chart ${chart}"
            return
        }
        print oshDiff
        sh "helm delete --purge ${chart} ||:"
        sleep 30
        sh "./tools/deployment/common/wait-for-pods.sh openstack 300 ||:"
        def values
        withEnv(["OPENSTACK_RELEASE=${RELEASE}",
                 "CONTAINER_DISTRO_VERSION=${DISTRO_VERSION}"]) {
            values = sh(script: "./tools/deployment/common/get-values-overrides.sh ${chart}",
                        returnStdout: true)
        }
        sh "helm upgrade --install ${chart} ./${chart} --namespace openstack ${values}"
        sh "git add -A && git commit -m 'Changes by ${configRevision}'"
    }
}


def getOverridesHash(tagOverridesList) {
    org.apache.commons.codec.digest.DigestUtils.sha256Hex(tagOverridesList.toString())
}

def getCombinations(tagOverridesList) {
    size = tagOverridesList.size()
    def combs = [] as Set
    for (i = size - 1; i >= 0; i--)  {
        tagOverridesList.eachPermutation {
            combs << it.subList(0, i).sort()
        }
    }
    combs = combs as List
    combs.sort { a, b -> b.size() <=> a.size() }
    return combs
}

def getImageName(image) {
    image.split('/')[-1].split('@')[0].split(':')[0]
}


def prepareImages(chart, tags) {
    def yaml = "openstack-helm/${chart}/values_overrides/${RELEASE}-ubuntu_${DISTRO_VERSION}.yaml"
    def data = readYaml(file: yaml)
    def defaultOverrides = tags.default ?: [:]
    def overrides = tags.overrides ?: [:]
    def images = [:]
    def newData = json.parseText(JsonOutput.toJson(data))
    data.images.tags.each { deployedTag, deployedImage ->
        if (!deployedImage.contains('mos-' << chart)) { return }
        print ("Processing ${deployedTag}")
        def tagOverrides = ((overrides[deployedTag] != null) ?
                            overrides[deployedTag] : defaultOverrides)
        tagOverridesList = []
        tagOverrides.each { k, v -> tagOverridesList.add([k, v])}
        tagOverridesList.sort()
        def overridesHash = getOverridesHash(tagOverridesList)
        def imageName = getImageName(deployedImage)
        def newImage = (imageName << ':' << overridesHash).toString()

        // populate DEFAULT_IMAGES to be a source of initial images in future
        if (!AVAILABLE_IMAGES.contains(deployedImage) && !DEFAULT_IMAGES[imageName]) {
            print "Adding ${deployedImage} to default images"
            DEFAULT_IMAGES[imageName] = deployedImage
        }
        if (!tagOverridesList) {
            newData.images.tags[deployedTag] = DEFAULT_IMAGES[imageName]
            return
        }

        // Continue if an image with required overrides is already deployed
        if (newImage == deployedImage) { return }

        // Create new image if image with required overrides does not exist
        // and add it to AVAILABLE_IMAGES
        def created = false
        if (!AVAILABLE_IMAGES.contains(newImage)) {
            print ("Hash of '${tagOverridesList}' is ${overridesHash}")
            // search for images with partial overrides and apply difference
            for (List item: getCombinations(tagOverridesList)) {
                def baseImage = (imageName << ':' << getOverridesHash(item)).toString()
                if (AVAILABLE_IMAGES.contains(baseImage)) {
                    print ("Found image with partially deployed overrides: \n${baseImage} - ${item}")
                    difference = tagOverridesList - item
                    print "Applying difference ${difference} to ${baseImage}"
                    createImage(newImage, difference, baseImage)
                    created = true
                    break
                }
            }
            // If image with partial overrides or base image with empty overrides
            // do not exist create base image and apply all overrides to it
            if (!created) {
                def defaultImage = DEFAULT_IMAGES[imageName]
                print "Creating a base image with empty overrides from ${defaultImage}"
                def baseImage = (imageName << ':' << getOverridesHash([])).toString()
                createImage(baseImage, [], defaultImage)
                AVAILABLE_IMAGES << baseImage
                print "Creating image with overrides ${tagOverridesList} from ${baseImage}"
                createImage(newImage, tagOverridesList, baseImage)
            }
            AVAILABLE_IMAGES << newImage
        } else {
            print "Image ${newImage} with overrides ${tagOverridesList} already exist"
        }
        newData.images.tags[deployedTag] = newImage
    }
    if (data != newData) {
        sh "rm ${yaml}"
        writeYaml(file: yaml, data: newData)
    }
}


def createImage(image, overrides, baseImage) {
    def cmd = ""
    def mounts = ""
    PY = 'python3'
    sshagent ([INTERNAL_GERRIT_KEY]) {
        overrides.each {
            def projectUrl = (it[0].contains("://") ? it[0] : "${INTERNAL_GERRIT_SSH}/${it[0]}")
            def refspec = it[1]
            def projectDir = projectUrl.split('/')[-1]
            if (!fileExists(projectDir)) {
                sh "git clone ${projectUrl}"
            } else {
                def remote
                dir (projectDir) {
                    remote = sh (
                        returnStdout: true,
                        script: 'git remote -v|grep origin|head -1|awk \'{print $2}\''
                    ).trim()
                }
                if (remote != projectUrl) {
                    sh "rm -rf ${projectDir}"
                    sh "git clone ${projectUrl}"
                }
            }
            dir (projectDir) {
                sh "git fetch origin ${refspec} && git checkout FETCH_HEAD"
            }
            cmd = cmd << "cd ${projectDir} && ${PY} setup.py install && cd /; "
            mounts = mounts << "-v ${WORKSPACE}/${projectDir}:/${projectDir} "
        }
    }
    cmd = "bash -c 'apt-get update && apt-get install -y git; ${cmd}'"
    sh "sudo rm containerid ||:"
    sh "sudo docker run --cidfile=containerid ${mounts} ${baseImage} ${cmd}"
    def containerId = readFile "containerid"
    sh "sudo docker commit ${containerId} ${image}"
    sh "sudo rm containerid"
}


if (CREATE_SNAPSHOT) {
    TestVm(initScript: 'bootstrap.sh',
           image: BASE_IMAGE,
           flavor: 'm1.xlarge',
           nodePostfix: 'create-osh-initial',
           buildType: 'basic',
           doNotDeleteNode: false,
           postBuildSuccessBody: createSnapshot,
           preBuildBody: setupNode,
    ) {
        K8S_DEPLOY_STEPS.each { it() }

        stage('Prepare node to snapshot creation') {
            sh "sudo docker logout ${ARTF_SECURE_DOCKER_URL}"
            cleanWs()
        }
        sh 'sudo bash -c "echo \'sleep 1; sudo poweroff;\' | at now"'
    }
}


def troubleshooting = { nodeName ->
    stage("Troubleshooting") {
        initConfig(nodeName)
        timeout (480) {
            readConfig(nodeName)
        }
    }
}


TestVm(initScript: 'bootstrap.sh',
       image: ((INITIAL_DEPLOYMENT && !CREATE_SNAPSHOT) ? BASE_IMAGE :
                SNAPSHOT_NAME << (CREATE_SNAPSHOT ? "-tmp": "")),
       flavor: 'm1.xlarge',
       nodePostfix: 'deploy-osh-aio',
       buildType: 'basic',
       doNotDeleteNode: false,
       timeout: (TROUBLESHOOTING ? 540 :
                 (INITIAL_DEPLOYMENT && !CREATE_SNAPSHOT ? 180 : 60)),
       label: LABEL,
       postBuildFailureBody: CREATE_SNAPSHOT ? deleteInitialSnapshot : {},
       postBuildSuccessBody: CREATE_SNAPSHOT ? replaceImage : {},
       preBuildBody: (INITIAL_DEPLOYMENT && !CREATE_SNAPSHOT) ? setupNode : null,
       troubleshootingBody: TROUBLESHOOTING ? troubleshooting : null,
) {
    if (INITIAL_DEPLOYMENT && !CREATE_SNAPSHOT) {
        K8S_DEPLOY_STEPS.each { it() }
    } else {
        stage('Authenticate docker repo') {
            utils.retrier(NET_RETRY_COUNT) {
                osh.dockerAuth(ARTF_READONLY_CREDS)
            }
        }
        stage('Clone OpenstackHelm') {
            cloneOSH()
            tweakOSH()
            // make helm toolkit explicitly here to replace the old
            // one from snapshot
            dir ("openstack-helm-infra") {
                sh "make helm-toolkit"
            }
        }
        stage('Override images') {
            // Override default OSH images from global manifests, RELEASE_OVERRIDES,
            // latest mos set and OVERRIDE_IMAGES map and creates override yamls
            // for every component.
            // Also replaces all mentiones of upstream registries to artifactory cache
            imageOverrides(overrideImagesMap)
        }
        stage('Install OpenstackClient') {
            installOpenstackClient()
        }
        stage('Wait for k8s cluster') {
            ['kube-system', 'ceph', 'openstack'].each {
                sh "./openstack-helm/tools/deployment/common/wait-for-pods.sh ${it} 1800"
            }
        }
    }
    stage('Install OSH AIO') {
        try {
            [
                ['MariaDB', 'Memcached', 'RabbitMQ'],
                ['Keystone'],
                ['Heat', 'Horizon', 'Rados GW'] + EXTRA_CHARTS,
                ['Glance', 'Cinder', 'Openvswitch'],
                ['Libvirt'],
                ['Compute Kit'],
                ['Gateway'],
            ].each {
                installOSHAIO(it)
            }
        } catch (Exception e) {
            artifactLogs()
            error "OSH AIO deployment failed with exception ${e}"
        }
    }
    sh "sudo docker logout ${ARTF_SECURE_DOCKER_URL}"
    if (!TROUBLESHOOTING) {
        stage('Run Helm tests') {
            try {
                runHelmTests(['nova', 'cinder', 'glance', 'heat', 'keystone', 'neutron'])
            } catch (Exception e) {
                artifactLogs()
                throw e
            }
        }
    }
}


def artifactLogs() {
    pip = 'pip3'
    sh "${pip} install 'ansible==2.9'"
    sh "sed -i 's/hosts: primary/hosts: localhost/g' openstack-helm-infra/playbooks/osh-infra-collect-logs.yaml"
    sh "ansible-playbook openstack-helm-infra/playbooks/osh-infra-collect-logs.yaml"
    cmd = "sudo tar --warning=no-file-changed -czf ${WORKSPACE}/artifacts/${BUILD_TAG}.tar.gz /tmp/logs"
    sh(script: cmd)
    archiveArtifacts 'artifacts/*'
}

def initConfig(nodeName) {
    node (nodeName) {
        sh "sudo usermod -aG docker ubuntu"
        sh "sudo bash -c 'echo \"OS_CLOUD=openstack_helm\" >> /etc/environment'"
        sh "sudo bash -c 'echo \"OPENSTACK_RELEASE=${RELEASE}\" >> /etc/environment'"
        sh "sudo bash -c 'echo \"CONTAINER_DISTRO_VERSION=${DISTRO_VERSION}\" >> /etc/environment'"
        workdir = "${HOME}/workdir"
        sh "mkdir ${workdir}"
        dir (workdir) {
            ["openstack-helm", "openstack-helm-infra"].each {
                sh "ln -s ${WORKSPACE}/${it} ${it}"
            }
        }
        sh "mkdir config"
        while (fileExists('config/lock')) { sleep 1 }
        sh "touch config/lock"
        dir ("config") {
            sh "git init"
            def deathTime = (System.currentTimeMillis() + 14400000).toString()
            writeFile(file: "deathTime", text: deathTime.bytes.encodeBase64().toString())
            writeFile(file: "config", text: '{}'.bytes.encodeBase64().toString())
            sh 'git config user.email "T-850@model.101"'
            sh 'git config user.name "T-850 Model 101"'
            sh "git add config; git commit -m 'Initial'"
        }
        dir ("openstack-helm") {
            sh 'git config user.email "T-850@model.101"'
            sh 'git config user.name "T-850 Model 101"'
            sh "git add -A; git commit -m 'Initial'"
        }
        sh "rm config/lock"
    }
}

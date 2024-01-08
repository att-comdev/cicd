import att.comdev.cicd.config.conf

/**
 * Setup docker within docker (to get latest docker version on Ubuntu 16.04)
 * requires setting {"storage-driver": "overlay2"} option in docker.json
 *
 * @param artifactoryURL The docker repository URL
 * @param artifactoryCred Credentialsid for authenticating the docker repository
 * @param containerName Name of the running Dind Container
 */
def initEnv(String artifactoryURL, String artifactoryCred, String containerName, Integer retry_count=3) {
    def shell = "sudo"
    if (containerName) {
        def opts = "--privileged --name ${containerName}" +
                   " -e HTTP_PROXY=${HTTP_PROXY} -e HTTPS_PROXY=${HTTP_PROXY}" +
                   " -e NO_PROXY=${NO_PROXY} "
        def mounts = '-v /var/lib/docker' +
                     ' -v $(pwd):/opt/loci'

        // cmd for running Docker in Docker
        shell = "sudo docker exec ${containerName}"

        utils.retrier(retry_count) {
            sh "sudo docker run -d ${opts} ${mounts} ${ARTF_DOCKER_URL}/${conf.DIND_IMAGE}"
        }
        sh "${shell} sh -cx 'apk update; apk add git'"
    }

    withCredentials([usernamePassword(credentialsId: artifactoryCred,
            usernameVariable: 'ARTIFACTORY_USER',
            passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
        opts = '-u $ARTIFACTORY_USER -p $ARTIFACTORY_PASSWORD'
        utils.retrier(retry_count) {
            sh "${shell} docker login ${opts} ${artifactoryURL}"
        }
    }
}

/**
 * Setup local web server
 * enables localized use of git/docker artifacts
 * exposes mounted volume /opt/loci/web
 * requires setting {"storage-driver": "overlay2"} option in docker.json
 *
 * @param containerName Name of the running Dind Container
 * @param port Local port for the ngnix service
 */
def runNginx(String containerName, String localPort, Integer retry_count=3) {
    def shell = 'sudo'
    def sourceDir = '$(pwd)/web'
    sh "mkdir -p ${sourceDir}"
    if (containerName) {
        // cmd for running Docker in Docker
        shell = "sudo docker exec ${containerName}"
        sourceDir = '/opt/loci/web'
    }

    def opts = "-d -v ${sourceDir}:/usr/share/nginx/html:ro"
    def port = "-p ${localPort}:80"
    utils.retrier(retry_count) {
        sh "${shell} docker run ${opts} ${port} ${ARTF_DOCKER_URL}/${conf.NGINX_IMAGE}"
    }
}

/**
 * Export the filesystem structure of a running container
 * Used for building loci images with Wheels export
 *
 * @param containerName Name of the running Dind Container
 * @param requirementsImage Requirements image used to create the wheels export
 */
def exportWheels(String containerName, String requirementsImage) {
    def shell = 'sudo'
    def sourceDir = '$(pwd)/web/images'
    sh "mkdir -p ${sourceDir}"

    if (containerName) {
        // cmd for running Docker in Docker
        shell = "sudo docker exec ${containerName}"
        sourceDir = "/opt/loci/${sourceDir}"
    }

    sh "${shell} docker create --name loci-wheels ${requirementsImage} bash"
    sh "${shell} docker export -o ${sourceDir}/wheels.tar loci-wheels"
    sh "${shell} chmod +r ${sourceDir}/wheels.tar"
}

/**
 * Retrieve project configs
 * Used as args for building loci images
 *
 * @param release Name of the openstack release
 * @param projectName Name of the openstack project
 * @return Map containing the project configs for projectName
 */
def getDependencies(String release, String projectName) {
    def project_config = [
        'ussuri': [
            'keystone':      ['PROFILES': 'fluent apache ldap',
                              'PIP_PACKAGES': 'python-openstackclient',
                              'DIST_PACKAGES': ' '],
            'heat':          ['PROFILES': 'fluent apache',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'curl'],
            'glance':        ['PROFILES': 'fluent glance ceph',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'horizon':       ['PROFILES': 'fluent horizon apache',
                              'PIP_PACKAGES': 'heat-dashboard',
                              'DIST_PACKAGES': ' '],
            'cinder':        ['PROFILES': 'fluent cinder lvm ceph qemu apache purestorage',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'neutron':       ['PROFILES': 'fluent neutron openvswitch linuxbridge apache',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'nova':          ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.0.0 net-tools'],
            'nova-1804':     ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.0.0 net-tools'],
            'barbican':      ['PROFILES': 'fluent',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': ' '],
            'neutron-sriov': ['PROFILES': 'fluent neutron linuxbridge openvswitch',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'placement':     ['PROFILES': 'apache',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'mysql-client'],
            'ironic':        ['PROFILES': 'fluent ipxe ipmi qemu tftp',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'ethtool lshw iproute2'],
        ],
        'victoria': [
            'keystone':      ['PROFILES': 'fluent apache ldap',
                              'PIP_PACKAGES': 'python-openstackclient',
                              'DIST_PACKAGES': ' '],
            'heat':          ['PROFILES': 'fluent apache',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'curl'],
            'glance':        ['PROFILES': 'fluent glance ceph',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'horizon':       ['PROFILES': 'fluent horizon apache',
                              'PIP_PACKAGES': 'heat-dashboard',
                              'DIST_PACKAGES': ' '],
            'cinder':        ['PROFILES': 'fluent cinder lvm ceph qemu apache purestorage',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'neutron':       ['PROFILES': 'fluent neutron openvswitch linuxbridge apache',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'nova':          ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.0.0 net-tools ovmf'],
            'nova-1804':     ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.0.0 net-tools ovmf'],
            'barbican':      ['PROFILES': 'fluent',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': ' '],
            'neutron-sriov': ['PROFILES': 'fluent neutron linuxbridge openvswitch',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'placement':     ['PROFILES': 'apache',
                              'PIP_PACKAGES': 'httplib2',
                              'DIST_PACKAGES': 'mysql-client'],
            'ironic':        ['PROFILES': 'fluent ipxe ipmi qemu tftp',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'ethtool lshw iproute2'],
        ],
        'wallaby': [
            'keystone':      ['PROFILES': 'fluent apache ldap',
                              'PIP_PACKAGES': 'python-openstackclient',
                              'DIST_PACKAGES': ' '],
            'heat':          ['PROFILES': 'fluent apache',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'curl'],
            'glance':        ['PROFILES': 'fluent glance ceph',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'horizon':       ['PROFILES': 'fluent horizon apache',
                              'PIP_PACKAGES': 'heat-dashboard',
                              'DIST_PACKAGES': ' '],
            'cinder':        ['PROFILES': 'fluent cinder lvm ceph qemu apache purestorage',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'neutron':       ['PROFILES': 'fluent neutron openvswitch linuxbridge apache',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'nova':          ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.1 net-tools ovmf'],
            'nova-1804':     ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.1 net-tools ovmf'],
            'barbican':      ['PROFILES': 'fluent',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': ' '],
            'neutron-sriov': ['PROFILES': 'fluent neutron linuxbridge openvswitch',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'placement':     ['PROFILES': 'apache',
                              'PIP_PACKAGES': 'httplib2',
                              'DIST_PACKAGES': 'mysql-client'],
            'ironic':        ['PROFILES': 'fluent ipxe ipmi qemu tftp',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'ethtool lshw iproute2'],
        ],
        'xena': [
            'keystone':      ['PROFILES': 'fluent apache ldap',
                              'PIP_PACKAGES': 'python-openstackclient',
                              'DIST_PACKAGES': ' '],
            'heat':          ['PROFILES': 'fluent apache',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'curl'],
            'glance':        ['PROFILES': 'fluent glance ceph',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'horizon':       ['PROFILES': 'fluent horizon apache',
                              'PIP_PACKAGES': 'heat-dashboard',
                              'DIST_PACKAGES': ' '],
            'cinder':        ['PROFILES': 'fluent cinder lvm ceph qemu apache purestorage',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'neutron':       ['PROFILES': 'fluent neutron openvswitch linuxbridge apache',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'nova':          ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.1 net-tools ovmf'],
            'nova-1804':     ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.1 net-tools ovmf'],
            'barbican':      ['PROFILES': 'fluent',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': ' '],
            'neutron-sriov': ['PROFILES': 'fluent neutron linuxbridge openvswitch',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'placement':     ['PROFILES': 'apache',
                              'PIP_PACKAGES': 'httplib2',
                              'DIST_PACKAGES': 'mysql-client'],
            'ironic':        ['PROFILES': 'fluent ipxe ipmi qemu tftp',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'ethtool lshw iproute2'],
        ],
        'yoga': [
            'keystone':      ['PROFILES': 'fluent apache ldap',
                              'PIP_PACKAGES': 'python-openstackclient',
                              'DIST_PACKAGES': ' '],
            'heat':          ['PROFILES': 'fluent apache',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'curl'],
            'glance':        ['PROFILES': 'fluent glance ceph',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'horizon':       ['PROFILES': 'fluent horizon apache',
                              'PIP_PACKAGES': 'heat-dashboard',
                              'DIST_PACKAGES': ' '],
            'cinder':        ['PROFILES': 'fluent cinder lvm ceph qemu apache purestorage',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'neutron':       ['PROFILES': 'fluent neutron openvswitch linuxbridge apache',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'nova':          ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.1 net-tools ovmf'],
            'nova-1804':     ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl1.1 net-tools ovmf'],
            'barbican':      ['PROFILES': 'fluent',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': ' '],
            'neutron-sriov': ['PROFILES': 'fluent neutron linuxbridge openvswitch',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'placement':     ['PROFILES': 'apache',
                              'PIP_PACKAGES': 'httplib2',
                              'DIST_PACKAGES': 'mysql-client'],
            'ironic':        ['PROFILES': 'fluent ipxe ipmi qemu tftp',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'ethtool lshw iproute2'],
        ],
        'antelope': [
            'keystone':      ['PROFILES': 'fluent apache ldap',
                              'PIP_PACKAGES': 'python-openstackclient',
                              'DIST_PACKAGES': ' '],
            'heat':          ['PROFILES': 'fluent apache',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'curl'],
            'glance':        ['PROFILES': 'fluent glance ceph',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'horizon':       ['PROFILES': 'fluent horizon apache',
                              'PIP_PACKAGES': 'heat-dashboard',
                              'DIST_PACKAGES': ' '],
            'cinder':        ['PROFILES': 'fluent cinder lvm ceph qemu apache purestorage',
                              'PIP_PACKAGES': 'python-swiftclient',
                              'DIST_PACKAGES': ' '],
            'neutron':       ['PROFILES': 'fluent neutron openvswitch linuxbridge apache',
                              'PIP_PACKAGES': 'networking-baremetal',
                              'DIST_PACKAGES': 'ethtool lshw jq'],
            'nova':          ['PROFILES': 'fluent nova ceph linuxbridge openvswitch configdrive qemu apache purestorage',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'libssl3 net-tools ovmf'],
            'barbican':      ['PROFILES': 'fluent',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': ' '],
            'placement':     ['PROFILES': 'apache',
                              'PIP_PACKAGES': 'httplib2',
                              'DIST_PACKAGES': 'mysql-client'],
            'ironic':        ['PROFILES': 'fluent ipxe ipmi qemu tftp',
                              'PIP_PACKAGES': '',
                              'DIST_PACKAGES': 'ethtool lshw iproute2'],
        ],
    ]

    return project_config[release][projectName]
}

/**
 * Merged maps specified in mapList into one map and compiles build arguments.
 *
 * @param mapList List containing maps with build parameters, merge priority increases with element index
 * @return mergedArgsMap Map Resulting map
 */
def mergeArgs(mapList) {
    mergedArgsMap = [:]
    mapList.each {
        if (it != null) {
            mergedArgsMap.putAll(it)
        }
    }
    return mergedArgsMap
}

/**
 * Create docker build arguments from provided map.
 *
 * @param paramMap Map Map that contains docker run parameters
 * @return args String containing ready to use parameter for docker run
 */
def buildParameters(paramMap) {
    args = ''
    paramMap.each { entry -> args += " --build-arg $entry.key='$entry.value'" }
    return args
}

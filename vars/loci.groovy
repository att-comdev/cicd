import att.comdev.cicd.config.conf

def project_config = [
      'keystone':            ['profiles': '"fluent apache ldap"',
                               'packages': '"pycrypto python-openstackclient"',
                               'distpackages': ' ',
                               'plugin_base_project': '',
                               'plugin_projects': [ ] ],
      'heat':                 ['profiles': '"fluent apache"',
                               'packages': 'pycrypto',
                               'distpackages': 'curl',
                               'plugin_base_project': '',
                               'plugin_projects': ['python-neutronclient', 'python-openstackclient', 'python-heatclient'] ],
      'glance':               ['profiles': '"fluent glance ceph"',
                               'packages': '"pycrypto python-swiftclient"',
                               'distpackages': ' ',
                               'plugin_base_project': '',
                               'plugin_projects': [ ] ],
      'horizon':              ['profiles': '"fluent horizon apache"',
                               'packages': 'pycrypto',
                               'distpackages': ' ',
                               'plugin_base_project': '',
                               'plugin_projects': [ ] ],
      'cinder':               ['profiles': '"fluent cinder lvm ceph qemu"',
                               'packages': '"pycrypto python-swiftclient"',
                               'distpackages': ' ',
                               'plugin_base_project': '',
                               'plugin_projects': [ ] ],
      'neutron':              ['profiles': '"fluent neutron openvswitch linuxbridge"',
                               'packages': 'pycrypto',
                               'distpackages': ' ',
                               'plugin_base_project': '',
                               'plugin_projects': ['tap-as-a-service'] ],
      'nova':                 ['profiles': '"fluent nova ceph linuxbridge openvswitch configdrive qemu apache"',
                               'packages': 'pycrypto',
                               'distpackages': ' ',
                               'plugin_base_project': '',
                               'plugin_projects': [ ] ],
      'barbican':             ['profiles': 'fluent',
                               'packages': 'pycrypto',
                               'distpackages': ' ',
                               'plugin_base_project': '',
                               'plugin_projects': [ ] ],
      'nova-1804':            ['profiles': '"fluent nova ceph linuxbridge openvswitch configdrive qemu apache"',
                               'packages': 'pycrypto',
                               'distpackages': 'libssl1.0.0',
                               'plugin_base_project': '',
                               'plugin_projects': [ ] ],
      'neutron-sriov':        ['profiles': '"fluent neutron linuxbridge openvswitch"',
                               'packages': 'pycrypto',
                               'distpackages': '"ethtool lshw"',
                               'plugin_base_project': '',
                               'plugin_projects': [ ] ],
      'tap-as-a-service':     ['profiles': '"fluent neutron linuxbridge openvswitch"',
                               'packages': 'pycrypto',
                               'distpackages': '"ethtool lshw"',
                               'plugin_base_project': 'neutron',
                               'plugin_projects': ['tap-as-a-service'] ],
      'python-neutronclient': ['profiles': '"fluent apache"',
                               'packages': 'pycrypto',
                               'distpackages': 'curl',
                               'plugin_base_project': 'heat',
                               'plugin_projects': ['python-neutronclient', 'python-openstackclient', 'python-heatclient'] ],
    ]

/**
 * Setup docker within docker (to get latest docker version on Ubuntu 16.04)
 * requires setting {"storage-driver": "overlay2"} option in docker.json
 *
 * @param artifactoryURL The docker repository URL
 * @param artifactoryCred Credentialsid for authenticating the docker repository
 * @param containerName Name of the running Dind Container
 */
def runDind(String artifactoryURL, String artifactoryCred, String containerName) {
    def opts = "--privileged --name ${containerName}" +
               " -e HTTP_PROXY=${HTTP_PROXY} -e HTTPS_PROXY=${HTTP_PROXY} "
    def mounts = '-v /var/lib/docker' +
                 ' -v $(pwd):/opt/loci'

    // cmd for running Docker in Docker
    dind = "sudo docker exec ${containerName}"

    sh "sudo docker run -d ${opts} ${mounts} ${ARTF_DOCKER_URL}/${conf.DIND_IMAGE}"
    sh "${dind} sh -cx 'apk update; apk add git'"

    withCredentials([usernamePassword(credentialsId: artifactoryCred,
            usernameVariable: 'ARTIFACTORY_USER',
            passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
        opts = '-u $ARTIFACTORY_USER -p $ARTIFACTORY_PASSWORD'
        sh "${dind} docker login ${opts} ${artifactoryURL}"
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
def runNginx(String containerName, String localPort) {
    sh "mkdir -p web"

    // cmd for running Docker in Docker
    dind = "sudo docker exec ${containerName}"

    def opts = '-d -v /opt/loci/web:/usr/share/nginx/html:ro'
    def port = "-p ${localPort}:80"
    sh "${dind} docker run ${opts} ${port} ${ARTF_DOCKER_URL}/${conf.NGINX_IMAGE}"
}

/**
 * Export the filesystem structure of a running container
 * Used for building loci images with Wheels export
 *
 * @param containerName Name of the running Dind Container
 * @param requirementsImage Requirements image used to create the wheels export
 */
def exportWheels(String containerName, String requirementsImage) {
    sh "mkdir -p web/images"

    // cmd for running Docker in Docker
    dind = "sudo docker exec ${containerName}"

    sh "${dind} docker create --name loci-wheels ${requirementsImage} bash"
    sh "${dind} docker export -o /opt/loci/web/images/wheels.tar loci-wheels"
    sh "${dind} chmod +r /opt/loci/web/images/wheels.tar"
}

/**
 * Retrieve project configs
 * Used as args for building loci images
 *
 * @param projectName Name of the openstack project
 * @return project_confs String containing the project configs as docker build args
 */
def getDependencies(String projectName) {
    project_confs = " --build-arg PROFILES=${project_config[projectName].profiles}\
                      --build-arg PIP_PACKAGES=${project_config[projectName].packages}\
                      --build-arg DIST_PACKAGES=${project_config[projectName].distpackages}"

    return project_confs
}

/**
 * Retrieve project's base project if any
 * Used for determining if project is a plugin to some other
 * base project during loci image build process.
 *
 * @param projectName Name of the openstack project
 * @return plugin_base_project String containing the project
 * name of base project if any.
 */
def getPluginBaseProject(String projectName) {
    return ${project_config[projectName].plugin_base_project}
}

/**
 * Retrieve project's plugins if any
 * Used for determining if project has any plugin(s) to install
 * during loci image build process.
 *
 * @param projectName Name of the openstack project
 * @return plugin_projects List of Strings containing the names
 * of the plugin projects to be installed in the loci image.
 */
def getPluginProjects(String projectName) {
    return ${project_config[projectName].plugin_projects}
}

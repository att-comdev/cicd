import com.att.nccicd.config.conf as ncconf

/**
 * Install Docker ce
 * Authenticate Artifactory docker repo
 * Copy docker config to root directory
 */
def installDockerCE() {
    sh '''sudo apt-get update && sudo apt-get install -y \\
          apt-transport-https \\
          ca-certificates \\
          curl \\
          software-properties-common'''

    sh 'curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -'
    sh '''sudo add-apt-repository \\
        "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"'''
    sh '''sudo apt-get update && \\
    sudo apt-get install -y docker-ce'''

    // Allow kubectl to pull images, requires auth config to be on / directory
        withCredentials([usernamePassword(credentialsId: 'jenkins-artifactory',
                    usernameVariable: 'ARTIFACTORY_USER',
                    passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
            opts = '-u $ARTIFACTORY_USER -p $ARTIFACTORY_PASSWORD'
            sh "sudo docker login ${opts} $ARTF_SECURE_DOCKER_URL"
            sh 'sudo cp -R ~/.docker /'
        }
}

/**
 * Create artifacts directory in $WORKSPACE
 */
def createArtifactsDir() {
        def artifact_dir = "$WORKSPACE/artifacts"
    if (!fileExists(artifact_dir)) {
        sh "mkdir $artifact_dir"
    }
}

/**
 * Clone OpenstackHelm projects into 'WORKSPACE'
 * Logs commmithash of latest revision into 'artifacts/OSH_version.txt'
 */
def cloneOSH() {
    createArtifactsDir()
    for (proj in ['openstack-helm', 'openstack-helm-infra']) {
        git_url = "https://git.openstack.org/openstack/${proj}.git"
        branch = "master"
        gerrit.cloneProject(git_url, branch, "", "${WORKSPACE}/${proj}")
        version = gerrit.getVersion(git_url, branch)
        sh "echo ${proj} head is at ${version} | tee -a ${WORKSPACE}/artifacts/OSH_version.txt"
    }
}

/**
 * Update proxy and dns for OpenstackHelm deployment
 */
def updateProxy() {
    sh "sed -i 's/8.8.8.8/${ARTF_IP}/g' ./openstack-helm-infra/tools/images/kubeadm-aio/assets/opt/playbooks/vars.yaml"
    def amap = ['kubernetes_network_default_device': 'docker0',
                 'gate_fqdn_test': 'true',
                'proxy': [ 'http': "${HTTP_PROXY}", 'https': "${HTTP_PROXY}", 'noproxy': "${NO_PROXY}"] ]
    sh 'rm -rf ./openstack-helm-infra/tools/gate/devel/local-vars.yaml'
    writeYaml file: './openstack-helm-infra/tools/gate/devel/local-vars.yaml', data: amap
}

/**
 * release specific image Overrides
 * <Openstackservice>_LOCI default is overridden by ps image built
 *
 * @param mos false - default - get community image versions
              true - get mos image versions
 */
def imageOverrides(Boolean mos = false ) {
    prefix = mos ? 'MOS_' : ''
    KEYSTONE_LOCI=(env.KEYSTONE_LOCI != null) ? env.KEYSTONE_LOCI : "ncconf.${prefix}KEYSTONE_LOCI"
    HEAT_LOCI=(env.HEAT_LOCI != null) ? env.HEAT_LOCI : "ncconf.${prefix}HEAT_LOCI"
    HORIZON_LOCI=(env.HORIZON_LOCI != null) ? env.HORIZON_LOCI : "ncconf.${prefix}HORIZON_LOCI"
    GLANCE_LOCI=(env.GLANCE_LOCI != null) ? env.GLANCE_LOCI : "ncconf.${prefix}GLANCE_LOCI"
    CINDER_LOCI=(env.CINDER_LOCI != null) ? env.CINDER_LOCI : "ncconf.${prefix}CINDER_LOCI"
    NOVA_LOCI=(env.NOVA_LOCI != null) ? env.NOVA_LOCI : "ncconf.${prefix}NOVA_LOCI"
    NOVA_1804_LOCI=(env.NOVA_1804_LOCI != null) ? env.NOVA_1804_LOCI : "ncconf.${prefix}NOVA_1804_LOCI"
    NEUTRON_LOCI=(env.NEUTRON_LOCI != null) ? env.NEUTRON_LOCI : "ncconf.${prefix}NEUTRON_LOCI"
    NEUTRON_SRIOV_LOCI=(env.NEUTRON_SRIOV_LOCI != null) ? env.NEUTRON_SRIOV_LOCI : "ncconf.${prefix}NEUTRON_SRIOV_LOCI"

    // supporting only ocata deployments for now
    def loci_yaml="${WORKSPACE}/openstack-helm/tools/overrides/releases/ocata/loci.yaml"

    def imagemap = [
      'images': [
        'tags': [
          'bootstrap': "$HEAT_LOCI",
          'cinder_api': "$CINDER_LOCI",
          'cinder_backup': "$CINDER_LOCI",
          'cinder_db_sync': "$CINDER_LOCI",
          'cinder_scheduler': "$CINDER_LOCI",
          'cinder_volume': "$CINDER_LOCI",
          'cinder_volume_usage_audit': "$CINDER_LOCI",
          'db_drop': "$HEAT_LOCI",
          'db_init': "$HEAT_LOCI",
          'glance_api': "$GLANCE_LOCI",
          'glance_bootstrap': "$HEAT_LOCI",
          'glance_db_sync': "$GLANCE_LOCI",
          'glance_registry': "$GLANCE_LOCI",
          'heat_api': "$HEAT_LOCI",
          'heat_cfn': "$HEAT_LOCI",
          'heat_cloudwatch': "$HEAT_LOCI",
          'heat_db_sync': "$HEAT_LOCI",
          'heat_engine': "$HEAT_LOCI",
          'heat_engine_cleaner': "$HEAT_LOCI",
          'horizon': "$HORIZON_LOCI",
          'horizon_db_sync': "$HORIZON_LOCI",
          'keystone_api': "$KEYSTONE_LOCI",
          'keystone_bootstrap': "$HEAT_LOCI",
          'keystone_credential_rotate': "$KEYSTONE_LOCI",
          'keystone_credential_setup': "$KEYSTONE_LOCI",
          'keystone_db_sync': "$KEYSTONE_LOCI",
          'keystone_domain_manage': "$KEYSTONE_LOCI",
          'keystone_fernet_rotate': "$KEYSTONE_LOCI",
          'keystone_fernet_setup': "$KEYSTONE_LOCI",
          'ks_endpoints': "$HEAT_LOCI",
          'ks_service': "$HEAT_LOCI",
          'ks_user': "$HEAT_LOCI",
          'neutron_db_sync': "$NEUTRON_LOCI",
          'neutron_dhcp': "$NEUTRON_LOCI",
          'neutron_l3': "$NEUTRON_LOCI",
          'neutron_linuxbridge_agent': "$NEUTRON_LOCI",
          'neutron_metadata': "$NEUTRON_LOCI",
          'neutron_openvswitch_agent': "$NEUTRON_LOCI",
          'neutron_server': "$NEUTRON_LOCI",
          'neutron_sriov_agent': "$NEUTRON_SRIOV_LOCI",
          'neutron_sriov_agent_init': "$NEUTRON_SRIOV_LOCI",
          'nova_api': "$NOVA_LOCI",
          'nova_cell_setup': "$NOVA_LOCI",
          'nova_cell_setup_init': "$HEAT_LOCI",
          'nova_compute': "$NOVA_1804_LOCI",
          'nova_compute_ironic': "$NOVA_LOCI",
          'nova_compute_ssh': "$NOVA_LOCI",
          'nova_conductor': "$NOVA_LOCI",
          'nova_consoleauth': "$NOVA_LOCI",
          'nova_db_sync': "$NOVA_LOCI",
          'nova_novncproxy': "$NOVA_LOCI",
          'nova_novncproxy_assets': "$NOVA_LOCI",
          'nova_placement': "$NOVA_LOCI",
          'nova_scheduler': "$NOVA_LOCI",
          'nova_spiceproxy': "$NOVA_LOCI",
          'nova_spiceproxy_assets': "$NOVA_LOCI",
          'scripted_test': "$HEAT_LOCI",
          'barbican_api': 'docker.io/openstackhelm/barbican:ocata',
          'barbican_db_sync': 'docker.io/openstackhelm/barbican:ocata',
          'congress_api': 'docker.io/openstackhelm/congress:ocata',
          'congress_datasource': 'docker.io/openstackhelm/congress:ocata',
          'congress_db_sync': 'docker.io/openstackhelm/congress:ocata',
          'congress_ds_create': 'docker.io/openstackhelm/congress:ocata',
          'congress_policy_engine': 'docker.io/openstackhelm/congress:ocata',
          'congress_scripted_test': 'docker.io/openstackhelm/congress:ocata',
          'ironic_api': 'docker.io/openstackhelm/ironic:ocata',
          'ironic_bootstrap': 'docker.io/openstackhelm/ironic:ocata',
          'ironic_conductor': 'docker.io/openstackhelm/ironic:ocata',
          'ironic_db_sync': 'docker.io/openstackhelm/ironic:ocata',
          'ironic_pxe': 'docker.io/openstackhelm/ironic:ocata',
          'ironic_pxe_init': 'docker.io/openstackhelm/ironic:ocata',
          'magnum_api': 'docker.io/openstackhelm/magnum:ocata',
          'magnum_conductor': 'docker.io/openstackhelm/magnum:ocata',
          'magnum_db_sync': 'docker.io/openstackhelm/magnum:ocata',
          'senlin_api': 'docker.io/openstackhelm/senlin:ocata',
          'senlin_db_sync': 'docker.io/openstackhelm/senlin:ocata',
          'senlin_engine': 'docker.io/openstackhelm/senlin:ocata',
          'senlin_engine_cleaner': 'docker.io/openstackhelm/senlin:ocata',
          'tempest': 'docker.io/kolla/ubuntu-source-tempest:4.0.3',
          'test': 'docker.io/kolla/ubuntu-source-rally:4.0.0'
        ]
      ]
    ]
    sh "rm -rf $loci_yaml"
    writeYaml file: loci_yaml, data: imagemap

    // update cirros image location to internal mirror to allow access from rally without proxy.
    // get glance test schema error while defining OSH_EXTRA_HELM_ARGS_GLANCE with --set overrides
    // replacing the url string for now
    sh """sed -i -e "s|http://download.cirros-cloud.net/0.3.5/|http://mirrors-aic.it.att.com/other_images/ops_simple_images/|" \\
        ${WORKSPACE}/openstack-helm/glance/values.yaml"""
}

/**
 * Deploy OSH AIO using developer scripts
 * uses ceph fs
 */
def installOSHAIO() {
    dir ('openstack-helm') {

        // see https://docs.openstack.org/openstack-helm/latest/install/developer/index.html
        def deploy_steps = ['Packages'   : './tools/deployment/developer/common/000-install-packages.sh',
                            'Kubernetes' : './tools/deployment/developer/common/010-deploy-k8s.sh',
                            'Clients'    : './tools/deployment/developer/common/020-setup-client.sh',
                            'Ingress'    : './tools/deployment/developer/common/030-ingress.sh',
                            'Ceph'       : './tools/deployment/developer/ceph/040-ceph.sh',
                            'Ceph NS'    : './tools/deployment/developer/ceph/045-ceph-ns-activate.sh',
                            'MariaDB'    : './tools/deployment/developer/ceph/050-mariadb.sh',
                            'RabbitMQ'   : './tools/deployment/developer/ceph/060-rabbitmq.sh',
                            'Memcached'  : './tools/deployment/developer/ceph/070-memcached.sh',
                            'Keystone'   : './tools/deployment/developer/ceph/080-keystone.sh',
                            'Heat'       : './tools/deployment/developer/ceph/090-heat.sh',
                            'Horizon'    : './tools/deployment/developer/ceph/100-horizon.sh',
                            'Rados GW'   : './tools/deployment/developer/ceph/110-ceph-radosgateway.sh',
                            'Glance'     : './tools/deployment/developer/ceph/120-glance.sh',
                            'Cinder'     : './tools/deployment/developer/ceph/130-cinder.sh',
                            'Openvswitch': './tools/deployment/developer/ceph/140-openvswitch.sh',
                            'LibVirt'    : './tools/deployment/developer/ceph/150-libvirt.sh',
                            'Compute Kit': './tools/deployment/developer/ceph/160-compute-kit.sh',
                            'Gateway'    : './tools/deployment/developer/ceph/170-setup-gateway.sh']

        deploy_steps.each { key, value ->
            print "Installing $key..."
            sh "${value}"
        }
    }
}

/**
 * Log openstack service versions into artifacts/openstack_versions.txt
 * @param mos false - default - get community image versions
              true - get mos image versions
 */
def serviceVersions(Boolean mos = false ) {
    prefix = mos ? 'MOS_' : ''

    createArtifactsDir()

    KEYSTONE_LOCI=(env.KEYSTONE_LOCI != null) ? env.KEYSTONE_LOCI : "ncconf.${prefix}KEYSTONE_LOCI"
    HEAT_LOCI=(env.HEAT_LOCI != null) ? env.HEAT_LOCI : "ncconf.${prefix}HEAT_LOCI"
    GLANCE_LOCI=(env.GLANCE_LOCI != null) ? env.GLANCE_LOCI : "ncconf.${prefix}GLANCE_LOCI"
    CINDER_LOCI=(env.CINDER_LOCI != null) ? env.CINDER_LOCI : "ncconf.${prefix}CINDER_LOCI"
    NOVA_1804_LOCI=(env.NOVA_1804_LOCI != null) ? env.NOVA_1804_LOCI : "ncconf.${prefix}NOVA_1804_LOCI"
    NEUTRON_LOCI=(env.NEUTRON_LOCI != null) ? env.NEUTRON_LOCI : "ncconf.${prefix}NEUTRON_LOCI"

    // omitting horizon as helm tests and cli do not exist
    def projmap = ['keystone' : ['image' : "$KEYSTONE_LOCI", 'cli' : 'keystone-manage'],
                   'heat'     : ['image' : "$HEAT_LOCI", 'cli' : 'heat-manage'],
                   'glance'   : ['image' : "$GLANCE_LOCI", 'cli' : 'glance-manage'],
                   'cinder'   : ['image' : "$CINDER_LOCI", 'cli' : 'cinder-manage'],
                   'nova'     : ['image' : "$NOVA_1804_LOCI", 'cli' : 'nova-manage'],
                   'neutron'  : ['image' : "$NEUTRON_LOCI", 'cli' : 'neutron-debug']]
    projmap.each { proj, value ->
        cmd = "sudo docker run --rm --name tempcont${proj} -t ${value.image} ${value.cli} --version"
        openstack_version = sh(returnStdout: true, script: cmd).trim()
        sh """echo "${proj} version is \\
              $openstack_version" | tee -a $WORKSPACE/artifacts/openstack_versions.txt"""
    }
}

/**
 * Run Helm tests on all Openstack services and log the results into artifacts/helm_tests.log
  */
def runHelmTests() {
    createArtifactsDir()

    for (proj in ['keystone', 'heat', 'glance', 'cinder', 'nova', 'neutron']) {
        sh """helm test --debug ${proj} >> $WORKSPACE/artifacts/helm_tests.log || \\
              kubectl logs ${proj}-test --namespace openstack | tee -a $WORKSPACE/artifacts/helm_tests.log"""
    }
}

/**
 * Parse Helm test logs for failures
 * @return status 0 for success
 *                1 for failure
 */
def parseTestLogs() {
    def testlog = readFile "$WORKSPACE/artifacts/helm_tests.log"
    if (testlog.find("FAILED:")) {
        return 1
    }
    return 0
}

/**
 * Archive var/log to assist in debugging failures
 * Archive 'artifacts' directory to allow access from jenkins job
 * @return status 0 for success
 *                1 for failure
 */
def artifactLogs() {
    cmd = "sudo tar --warning=no-file-changed -czf ${WORKSPACE}/artifacts/${BUILD_TAG}.tar.gz /var/log"
    sh(returnStdout: true, script: cmd)
    archiveArtifacts 'artifacts/*'
}

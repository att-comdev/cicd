def AIRSHIPCTL
def AIRSHIP_SRC
def KUBECONFIG
def WORKERS_COUNT
def CONTROLPLANE_COUNT
def TEST_SITE
def TARGET_CLUSTER_NAME
def KIND_EXPERIMENTAL_DOCKER_NETWORK
def PROVIDER
node(targetVM) {
	withEnv(['AIRSHIP_SRC=/tmp/airship',
		'KUBECONFIG=$HOME/.airship/kubeconfig',
		'WORKERS_COUNT=2',
		'CONTROLPLANE_COUNT=1',
		'TEST_SITE=openstack-test-site',
		'TARGET_CLUSTER_NAME=target-cluster',
		'EPHEMERAL_CLUSTER_NAME=ephemeral-cluster',
		'KIND_EXPERIMENTAL_DOCKER_NETWORK=bridge',
		'AIRSHIPCTL=/tmp/airship/airshipctl',
        'PROVIDER_MANIFEST=openstack_manifest',
		'PROVIDER=default' ]){
	try{
		AIRSHIPCTL = sh(script: """echo /tmp/airship/airshipctl""", returnStdout:true).trim()
		stage("Clone Airshipctl") {
			writeFile file: 'clone.sh', text: '''#!/bin/bash
			sudo swapoff -a
			sudo rm -rf ~/.airship
			sudo rm -rf /tmp/airship
			mkdir /tmp/airship
			cd /tmp/airship
			git clone https://opendev.org/airship/airshipctl.git $AIRSHIPCTL
			cd $AIRSHIPCTL
			git fetch https://review.opendev.org/airship/airshipctl '''+ env.REF_SPEC_SCRIPTS +''' && git checkout FETCH_HEAD
            sed -i 's/value:.*/value: 6f02d568-b46d-44ac-86e2-fc88b167fa90/g' manifests/site/openstack-test-site/ephemeral/controlplane/external_network_id.json
            sed -i 's/clouds.yaml:.*/clouds.yaml: '''+ env.CLOUDS_YAML_B64 +'''/g' manifests/site/openstack-test-site/ephemeral/controlplane/cluster_clouds_yaml_patch.yaml
            sed -i 's/clouds.yaml:.*/clouds.yaml: '''+ env.CLOUDS_YAML_B64 +'''/g' manifests/site/openstack-test-site/target/workers/cluster_clouds_yaml_patch.yaml
            sed -i 's/content:.*/content: '''+ env.CLOUDS_CONF_B64 +'''/' manifests/site/openstack-test-site/ephemeral/controlplane/control_plane_config_patch.yaml
            sed -i 's/content:.*/content: '''+ env.CLOUDS_CONF_B64 +'''/' manifests/site/openstack-test-site/target/workers/workers_cloud_conf_patch.yaml
            sed -i 's#ssh-rsa:.*#ssh-rsa: '''+ env.SSH_PUB_KEY +'''#g' manifests/site/openstack-test-site/ephemeral/controlplane/ssh_key_patch.yaml
            sed -i 's#ssh-rsa:.*#ssh-rsa: '''+ env.SSH_PUB_KEY +'''#g' manifests/site/openstack-test-site/target/workers/workers_ssh_key_patch.yaml
			'''
			sh 'bash ./clone.sh'
		}
		stage("Install Kustomize, Kind and KubeCtl") {
			writeFile file: 'install.sh', text: '''#!/bin/bash
			cd $AIRSHIPCTL &&  ./tools/gate/00_setup.sh
			curl -sSL https://github.com/kubernetes-sigs/kustomize/releases/download/kustomize/v3.8.5/kustomize_v3.8.5_linux_amd64.tar.gz | tar -C /tmp -xzf -
			sudo install /tmp/kustomize /usr/local/bin
			kustomize version
			./tools/deployment/provider_common/01_install_kind.sh
			./tools/deployment/01_install_kubectl.sh'''
			sh 'bash ./install.sh'
		}
		stage("install Airshipctl") {
			writeFile file: 'airshipctl.sh', text: '''#!/bin/bash
			cd $AIRSHIPCTL && ./tools/deployment/21_systemwide_executable.sh'''
			sh 'bash ./airshipctl.sh'
		}
		stage("Generate Airship Config File"){
			writeFile file: 'config.sh', text: '''#!/bin/bash
			cd $AIRSHIPCTL && ./tools/deployment/provider_common/03-init-airship-config.sh'''
			sh 'bash ./config.sh'
		}
		stage("Initialize Kind Cluster"){
			writeFile file: 'kind.sh', text: '''#!/bin/bash
			echo "deleting existing clusters" && kind delete clusters --all
			cd $AIRSHIPCTL && CLUSTER=ephemeral-cluster KIND_CONFIG=./tools/deployment/templates/kind-cluster-with-extramounts ./tools/document/start_kind.sh
			# cd $AIRSHIPCTL && AIRSHIP_CONFIG_METADATA_PATH=manifests/site/openstack-test-site/metadata.yaml SITE=openstack-test-site EXTERNAL_KUBECONFIG="true" ./tools/deployment/22_test_configs.sh'''
			sh 'bash ./kind.sh'
		}
		stage("Deploy Ephermal"){
			writeFile file: 'epheremal.sh', text: '''#!/bin/bash
			cd $AIRSHIPCTL && PROVIDER=default TEST_SITE=openstack-test-site PROVIDER_MANIFEST=openstack_manifest ./tools/deployment/26_deploy_capi_ephemeral_node.sh'''
			sh 'bash ./epheremal.sh'
		}
		stage("Deploy Control Plane"){
			writeFile file: 'control-plane.sh', text: '''#!/bin/bash
 			cd $AIRSHIPCTL && CONTROLPLANE_COUNT=1 TEST_SITE=openstack-test-site ./tools/deployment/provider_common/30_deploy_controlplane.sh'''
			sh 'bash ./control-plane.sh'
		}
		stage("Clusterctl Init Target"){
			writeFile file: 'cluster-init.sh', text: '''#!/bin/bash
			cd $AIRSHIPCTL && KUBECONFIG=/tmp/target-cluster.kubeconfig ./tools/deployment/provider_common/32_cluster_init_target_node.sh'''
			sh 'bash ./cluster-init.sh'
		}
		stage("Clusterctl Move"){
			writeFile file: 'cluster-move.sh', text: '''#!/bin/bash
			cd $AIRSHIPCTL && KUBECONFIG=/tmp/target-cluster.kubeconfig ./tools/deployment/provider_common/33_cluster_move_target_node.sh'''
			sh 'bash ./cluster-move.sh'
		}
		stage("Deploy Worker Node"){
			writeFile file: 'worker-node.sh', text: '''#!/bin/bash
			cd $AIRSHIPCTL && WORKERS_COUNT="2" KUBECONFIG="/tmp/target-cluster.kubeconfig" TEST_SITE="openstack-test-site" ./tools/deployment/provider_common/34_deploy_worker_node.sh'''
			sh 'bash ./worker-node.sh'
		}
	}catch (err) {
		echo "Failed: ${err}"
		currentBuild.result = 'FAILURE'
	}
}
}
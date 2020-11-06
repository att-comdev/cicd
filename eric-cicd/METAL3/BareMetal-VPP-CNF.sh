def AIRSHIPCTL
def HOME_DIR
def BUILDGATEYML
def KUBEADMCONFIGTEMPLATE

    node(targetVM) {
        AIRSHIPCTL = sh(script: """echo ~/airshipctl/""", returnStdout:true).trim()
        HOME_DIR = sh(script: """echo ~""", returnStdout:true).trim()
        BUILDGATEYML = sh(script: """echo ~/airshipctl/playbooks/airship-airshipctl-build-gate.yaml""", returnStdout:true).trim()
        KUBEADMCONFIGTEMPLATE = sh(script: """echo ~/airshipctl/manifests/site/test-site/target/workers/kubeadmconfigtemplate.yaml""", returnStdout:true).trim()
        stage("Cloning Airshipctl") {
          writeFile file: 'cleanup.sh', text: '''#!/bin/bash
            echo "Clean up AirshipCTL before proceeding !!!"
            export AIRSHIPCTL="/home/airship/airshipctl"
            if [[ -d  $AIRSHIPCTL ]] ; then
               echo "AirshipCTL dir is existing !!!"
               ls /home/airship/airshipctl/tools/deployment/clean.sh
               sudo bash /home/airship/airshipctl/tools/deployment/clean.sh
               sudo rm -rf $AIRSHIPCTL
               
            else
              echo "AirshipCTL does not exist !!!"
              echo "please clone it"
              
            fi '''
            sh 'bash ./cleanup.sh'   
            sh label: '', script: """git clone https://opendev.org/airship/airshipctl.git ${AIRSHIPCTL} \\
            && cd $AIRSHIPCTL && git fetch https://review.opendev.org/airship/airshipctl ${GERRIT_REFSPEC} && git checkout FETCH_HEAD"""
            
        }
        stage("Setting up Airshipctl") {
          sh label: '', script: """ \\
          [ -f ~/.ssh/id_rsa ] && echo "ssh key exists" || ssh-keygen -t rsa -f ~/.ssh/id_rsa -q -P "" && \\
          sed -i "s/\\(\\s*worker_disk_size\\):\\s*[0-9]*/\\1: 120/" ${BUILDGATEYML} && \\
          sed -i "s/\\(\\s*worker_vm_memory_mb\\):\\s*[0-9]*/\\1: 35840/" ${BUILDGATEYML} && \\
          sed -i "s/\\(\\s*worker_vm_vcpus\\):\\s*[0-9]*/\\1: 20/" ${BUILDGATEYML} && \\
          ssh_key=`cat ~/.ssh/id_rsa.pub` && \\
          sed -i "s|\\(ssh-rsa\\).*|\$ssh_key|" ${KUBEADMCONFIGTEMPLATE} \\
          && cd $AIRSHIPCTL &&  sudo -S ./tools/gate/00_setup.sh \\
          &&  sudo -S ./tools/gate/10_build_gate.sh && sudo -S ./tools/deployment/21_systemwide_executable.sh \\
          &&  sudo sed -i "s|/tmp/airship|${HOME_DIR}|g" ./tools/deployment/22_test_configs.sh \\
          &&  sudo -S ./tools/deployment/22_test_configs.sh"""
        }
        stage("Setting up Ephermal Node") {
          sh label: '', script: """cd $AIRSHIPCTL &&  sudo -S ./tools/deployment/24_build_ephemeral_iso.sh \\
          && sudo -S ./tools/deployment/25_deploy_ephemeral_node.sh"""
        }
        stage("Setting up MGMT Cluster with Metal3_CAPI") {
          sh label: '', script: """cd $AIRSHIPCTL &&  sudo -S ./tools/deployment/26_deploy_metal3_capi_ephemeral_node.sh"""
        }
        stage("Setting up Target Cluster with Metal3_CAPI") {
          sh label: '', script: """cd $AIRSHIPCTL &&  sudo -S ./tools/deployment/30_deploy_controlplane.sh \\
          && sudo -S ./tools/deployment/31_deploy_initinfra_target_node.sh \\
          && sudo -S ./tools/deployment/32_cluster_init_target_node.sh"""
        }
        
        stage("Move Target Node and Deploy Worker Node") {
          sh label: '', script: """cd $AIRSHIPCTL &&  sudo -S ./tools/deployment/33_cluster_move_target_node.sh \\
          && sudo -S ./tools/deployment/34_deploy_worker_node.sh"""
        }
        
        
        stage("Deploy and validate Sample CNF - VPP IPforwarder") {
          writeFile file: 'vpp.sh', text: '''#!/bin/bash
          set -xe
          export KUBECONFIG=${KUBECONFIG:-"$HOME/.airship/kubeconfig"}
          export KUBECONFIG_TARGET_CONTEXT=${KUBECONFIG_TARGET_CONTEXT:-"target-cluster"}
          export WORKLOAD_DIR=${WORKLOAD_DIR:-"$HOME/airshipctl/manifests/site/test-site/target/workload"}
          export VPPCNF_DIR=${VPPCNF_DIR:-"$HOME/workload/"}
          export TIMEOUT=${TIMEOUT:-60s}
          echo "Deploy IP Forwarder workload"
          cp -r $VPPCNF_DIR/vpp $WORKLOAD_DIR
          cp -r $VPPCNF_DIR/multus $WORKLOAD_DIR
          cp $VPPCNF_DIR/kustomization.yaml-vpp $WORKLOAD_DIR/kustomization.yaml
          cp $VPPCNF_DIR/35_deploy_workload_vpp.sh /home/airship/airshipctl/tools/deployment/'''
        sh 'bash ./vpp.sh'
        sh label: '', script: """cd $AIRSHIPCTL &&  sudo -S ./tools/deployment/35_deploy_workload_vpp.sh""" 
        validate_VPP_IP_Fwder_CNF()
          
        }
        
       
        stage("Clean Up") {
          writeFile file: 'endcleanup.sh', text: ''' #!/bin/bash
            export AIRSHIPCTL="/home/airship/airshipctl"
            ls /home/airship/airshipctl/tools/deployment/clean.sh
            sudo bash /home/airship/airshipctl/tools/deployment/clean.sh
            sudo rm -rf $AIRSHIPCTL '''
          sh 'bash ./endcleanup.sh'
          
        }
    }
    
    def validate_VPP_IP_Fwder_CNF() {
  sh label: '', script: '''pod_name=$(kubectl get pods --kubeconfig ~/.airship/kubeconfig  --context target-cluster -n ipforwarder -o jsonpath={.items[*].metadata.name})
    echo "PodName: $pod_name"
    container_names=$(kubectl get pods ${pod_name} --kubeconfig ~/.airship/kubeconfig  --context target-cluster -n ipforwarder -o jsonpath={.spec.containers[*].name})
    itr=1
    sleep 180
    for container in $(echo $container_names | tr " " "\n")
     do ip=$(echo "10.0.0.$itr/24")
     kubectl --kubeconfig ~/.airship/kubeconfig  --context target-cluster exec -i $pod_name -c $container -n ipforwarder vppctl set int ip addr host-net1 $ip
     itr=$((itr+1))
    done

    for container in $(echo $container_names | tr " " "\n")
     do
         itr=$((itr-1))
     ip=$(echo "10.0.0.$itr")
     kubectl --kubeconfig ~/.airship/kubeconfig  --context target-cluster exec -i $pod_name -c $container -n ipforwarder vppctl ping $ip | tee ping.txt
     cat ping.txt | grep bytes
         retval=$?
     if [ $retval -ne 0 ]; then
       echo "FAILED: Ping test between containers ($container_names) in pod $pod_name"
       exit $retval
     fi
    done'''
}


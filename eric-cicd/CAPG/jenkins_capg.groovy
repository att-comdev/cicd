podTemplate(yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    provider: capz
spec:
  securityContext:
    runAsUser: 0
  containers:
  - name: ubuntu-bionic
    image: ubuntu:18.04
    command:
    - cat
    tty: true
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
    volumeMounts:
    - name: home
      mountPath: /root
  volumes:
  - name: home
    emptyDir: {}
"""
) {
  node(POD_LABEL) {
    try{
      def az = [:]
      env.JOBNAME = JOB_NAME.toLowerCase()
      container('ubuntu-bionic') {
        stage("Setup Remote VM") {
          sh label: '', script: '''apt-get update; apt-get install -y git sudo make curl wget systemd vim jq && \\
            curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash'''
          withCredentials([azureServicePrincipal('AZURE_CLOUD_ERIC')]) {
            sh label: '', script: '''#!/bin/bash
              az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID;
              az group create --name capg-${JOBNAME}-${BUILD_NUMBER}-rg --location "${REMOTE_VM_REGION}";

              host=$(az vm create -g capg-${JOBNAME}-${BUILD_NUMBER}-rg \\
                -n capg-${JOBNAME}-${BUILD_NUMBER}-vm --image UbuntuLTS \\
                --authentication-type all \\
                --admin-username azureuser \\
                --admin-password "Azure-12345!" \\
                --generate-ssh-keys --query publicIpAddress \\
                --size ${REMOTE_VM_TYPE} -o tsv)
              echo $host >az-host-info
              cp /root/.ssh/id_rsa az-identity;
              ls -ltr '''
          }
          az.host=readFile('az-host-info').trim()
          az.name='azure-host'
          az.user='azureuser'
          az.allowAnyHosts=true
          az.identity=readFile('az-identity').trim()
          az.logLevel='INFO'

          writeFile file: 'setup.sh', text: 'sudo apt-get update && sudo apt-get install -y git sudo make wget systemd vim && \
            sudo apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common jq && \
            sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add - && sudo apt-key fingerprint 0EBFCD88 && \
            sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" && \
            export USER=`whoami` && sudo apt-get update && sudo apt-get install -y docker-ce docker-ce-cli containerd.io && \
            sudo usermod -aG docker $USER && mkdir -p airship'

          sshScript remote: az, script: "setup.sh"
          sh label: '', script: '''chmod +x ./setup.sh && ./setup.sh'''
        }
        stage("Clone Airshipctl Repo") {
          writeFile file: 'cloneRepo.sh', text: '''#!/bin/bash
            export PATH=$PATH:/usr/local/go/bin/ && \
            cd airship && git clone https://opendev.org/airship/airshipctl.git && \
            cd airshipctl && git fetch https://review.opendev.org/airship/airshipctl '''+ env.REF_SPEC_MANIFESTS +''' && git checkout FETCH_HEAD
            sed -i 's/<project_id>/"'''+ env.GCP_PROJECT +'''"}/g' manifests/site/gcp-test-site/target/controlplane/project_name.json
            sed -i 's/"value":.*/"value": "'''+ env.GCP_NETWORK_NAME +'''"}/g' manifests/site/gcp-test-site/ephemeral/controlplane/network_name.json
            sed -i 's/"value":.*/"value": "'''+ env.GCP_REGION +'''"}/g' manifests/site/gcp-test-site/target/controlplane/region_name.json
            sed -i 's/"value":.*/"value": "'''+ env.GCP_CONTROL_PLANE_MACHINE_TYPE +'''"}/g' manifests/site/gcp-test-site/target/controlplane/machine_type.json
            sed -i 's/"value":.*/"value": "'''+ env.GCP_CONTROL_PLANE_MACHINE_COUNT +'''"}/g' manifests/site/gcp-test-site/target/controlplane/machine_count.json
            sed -i 's/"value":.*/"value": "'''+ env.GCP_NODE_MACHINE_TYPE +'''"}/g' manifests/site/gcp-test-site/target/workers/machine_type.json
            sed -i 's/"value":.*/"value": "'''+ env.GCP_NODE_MACHINE_COUNT +'''"}/g' manifests/site/gcp-test-site/target/workers/machine_count.json
            '''
          sshScript remote: az, script: "cloneRepo.sh"
          sh label: '', script: '''chmod +x ./cloneRepo.sh && ./cloneRepo.sh'''
        }
        withCredentials([string(credentialsId: 'gcp-service-account', variable: 'GCP_B64ENCODED_CREDENTIALS')]) {
          stage("Setup Env Vars") {
            writeFile file: 'env.sh', text: 'export GCP_B64ENCODED_CREDENTIALS='+ env.GCP_B64ENCODED_CREDENTIALS +''
            writeFile file: 'site.sh', text: '''
               export GCP_PROJECT='''+ env.GCP_PROJECT +'''
               export GCP_REGION='''+ env.GCP_REGION +'''
               export GCP_NETWORK_NAME='''+ env.GCP_NETWORK_NAME +'''
               export GCP_CONTROL_PLANE_MACHINE_TYPE='''+ env.GCP_CONTROL_PLANE_MACHINE_TYPE +'''
               export GCP_NODE_MACHINE_TYPE='''+ env.GCP_NODE_MACHINE_TYPE +'''

               export GCP_PROJECT_B64=$( echo ${GCP_PROJECT} | base64 | tr -d '\n' )
               export GCP_REGION_B64=$( echo ${GCP_REGION} | base64 | tr -d '\n' )
               export GCP_NETWORK_NAME_B64=$( echo ${GCP_NETWORK_NAME} | base64 | tr -d '\n' )
               export GCP_CONTROL_PLANE_MACHINE_TYPE_B64=$( echo ${GCP_CONTROL_PLANE_MACHINE_TYPE} | base64 | tr -d '\n' )
               export GCP_NODE_MACHINE_TYPE_B64=$( echo ${GCP_NODE_MACHINE_TYPE} | base64 | tr -d '\n' )

               export SITE="gcp-test-site"
               export TEST_SITE=${SITE}
               export PROVIDER_MANIFEST="gcp_manifest"
               export PROVIDER="default"
               export CLUSTER="ephemeral-cluster"
               export EPHEMERAL_KUBECONFIG_CONTEXT="${CLUSTER}"
               export EPHEMERAL_CLUSTER_NAME="kind-${EPHEMERAL_KUBECONFIG_CONTEXT}"
               export EPHEMERAL_KUBECONFIG="${HOME}/.airship/kubeconfig"
               export TARGET_CLUSTER_NAME="target-cluster"
               export TARGET_KUBECONFIG_CONTEXT="${TARGET_CLUSTER_NAME}"
               export TARGET_KUBECONFIG="/tmp/${TARGET_CLUSTER_NAME}.kubeconfig"'''
            sh label: '', script: '''
              cat site.sh | sed -e 's/^[ \t]*//' >> env.sh
            '''
            sshPut remote: az, from: 'env.sh', into: 'airship'
          }
          stage("Configure Tools") {
            writeFile file: 'configureTools.sh', text: '''#!/bin/bash
            source airship/env.sh
            cd airship/airshipctl
            sudo -E AIRSHIP_SRC=`pwd`/../ AIRSHIPCTL_WS=`pwd` ./tools/deployment/azure/202_install_tools.sh
            '''
            sshScript remote: az, script: "configureTools.sh"
          }
          stage("CAPI & CAPZ Init on Ephemeral Cluster") {
            writeFile file: 'initEphemeral.sh', text: '''#!/bin/bash
            source airship/env.sh
            cd airship/airshipctl
            sudo -E AIRSHIP_SRC=`pwd`/../ AIRSHIPCTL_WS=`pwd` ./tools/deployment/phases/phase-clusterctl-init-ephemeral-script.sh
            '''
            sshScript remote: az, script: "initEphemeral.sh"
          }
          stage("Deploy Control Plane") {
            writeFile file: 'deployControlPlane.sh', text: '''#!/bin/bash
            source airship/env.sh
            cd airship/airshipctl
            sudo -E AIRSHIP_SRC=`pwd`/../ AIRSHIPCTL_WS=`pwd` ./tools/deployment/phases/phase-controlplane-ephemeral-script.sh
            '''
            sshScript remote: az, script: "deployControlPlane.sh"
          }
          stage("Init Infra - Calico CNI") {
            writeFile file: 'initInfra.sh', text: '''#!/bin/bash
            source airship/env.sh
            cd airship/airshipctl
            sudo -E AIRSHIP_SRC=`pwd`/../ AIRSHIPCTL_WS=`pwd` ./tools/deployment/phases/phase-initinfra-target-script.sh
            '''
            sshScript remote: az, script: "initInfra.sh"
          }
          stage("CAPI & CAPZ Init on Target Cluster") {
            writeFile file: 'initTarget.sh', text: '''#!/bin/bash
            source airship/env.sh
            cd airship/airshipctl
            sudo -E AIRSHIP_SRC=`pwd`/../ AIRSHIPCTL_WS=`pwd` ./tools/deployment/phases/phase-clusterctl-init-target-script.sh
            '''
            sshScript remote: az, script: "initTarget.sh"
          }
          stage("Move Resources to Target Cluster") {
            writeFile file: 'moveResources.sh', text: '''#!/bin/bash
            source airship/env.sh
            cd airship/airshipctl
            sudo -E AIRSHIP_SRC=`pwd`/../ AIRSHIPCTL_WS=`pwd` ./tools/deployment/phases/phase-clusterctl-move-script.sh
            '''
            sshScript remote: az, script: "moveResources.sh"
          }
          stage("Deploy Worker Nodes") {
            writeFile file: 'deployWorkers.sh', text: '''#!/bin/bash
            source airship/env.sh
            cd airship/airshipctl
            sudo -E AIRSHIP_SRC=`pwd`/../ AIRSHIPCTL_WS=`pwd` ./tools/deployment/phases/phase-workers-target-script.sh
            '''
            sshScript remote: az, script: "deployWorkers.sh"
          }
          stage("Cleanup Resources") {
            sh label: '', script: '''#!/bin/bash
              echo "+++++++++++ Cleaning up Resources ...."
              az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID;
              az group delete --name capg-${JOBNAME}-${BUILD_NUMBER}-rg --yes
              # Replace by Google Cloud CLI here [az group delete --name '''+ env.CLUSTER_NAME +'''-rg --yes]
              '''
          }
        }
      }
    }catch (err) {
      echo "Failed: ${err}"
      currentBuild.result = 'FAILURE'
    }
  }
}

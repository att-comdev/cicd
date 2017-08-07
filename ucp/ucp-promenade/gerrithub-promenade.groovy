def GENESIS_IP=""
def MASTER_1_IP=""
def MASTER_2_IP=""
def WORKER_IP=""
def BUILD_HOST=""
def G_HOST_IFACE=""
def M1_HOST_IFACE=""
def M2_HOST_IFACE=""
def W_IFACE=""

properties([[$class: 'BeforeJobSnapshotJobProperty'], parameters([string(defaultValue: '0', description: '', name: 'MAJOR_VERSION'), string(defaultValue: '1', description: '', name: 'MINOR_VERSION'), string(defaultValue: '0', description: '', name: 'PATCH_VERSION')]), pipelineTriggers([])])

node {
  // Checkout the Gerrit git repository using the existing
  // workflow git step...
  git url: 'ssh://jenkins-attcomdev@review.gerrithub.io:29418/att-comdev/promenade'

  // Fetch the changeset to a local branch using the build parameters provided to the
  // build by the Gerrit plugin...
  def changeBranch = "change-${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}"
  sh "git fetch origin ${GERRIT_REFSPEC}:${changeBranch}"
  sh "git checkout ${changeBranch}"
 
  // Build the changeset rev source etc...
}


stage('Provision Virtual Machines'){
    parallel (
    "stream 1" : { 
         node ('prom-node-genesis') {        
            GENESIS_IP = sh(returnStdout: true, script:'echo $OPENSTACK_PUBLIC_IP').trim()
            if(GENESIS_IP == "") {
                echo("Fail the Build")
            }
            G_HOST_IFACE = sh(returnStdout: true, script: 'echo $(ip route | grep \"^default\" | head -1 | awk \'{ print $5 }\')').trim()
            if(G_HOST_IFACE == ""){
                echo("Fail the Build")
            }
            G_HOSTNAME=sh(returnStdout: true, script: 'echo $(hostname)').trim()
            if(G_HOSTNAME == ""){
                echo("Fail the Build")
            }
        } 
    },
    "stream 2" : { 
        node ('prom-node-master-1') { 
            MASTER_1_IP = sh(returnStdout: true, script:'echo $OPENSTACK_PUBLIC_IP').trim()
            M1_HOST_IFACE = sh(returnStdout: true, script: 'echo $(ip route | grep \"^default\" | head -1 | awk \'{ print $5 }\')').trim()
            if(M1_HOST_IFACE == ""){
                echo("Fail the Build")
            }
            M1_HOSTNAME=sh(returnStdout: true, script: 'echo $(hostname)').trim()
            if(M1_HOSTNAME == ""){
                echo("Fail the Build")
            }
        } 
    },
    "stream 3" : { 
        node ('prom-node-master-2') {  
            MASTER_2_IP = sh(returnStdout: true, script:'echo $OPENSTACK_PUBLIC_IP').trim()
            M2_HOST_IFACE = sh(returnStdout: true, script: 'echo $(ip route | grep \"^default\" | head -1 | awk \'{ print $5 }\')').trim()
            if(M2_HOST_IFACE == ""){
                echo("Fail the Build")
            }
            M2_HOSTNAME=sh(returnStdout: true, script: 'echo $(hostname)').trim()
            if(M2_HOSTNAME == ""){
                echo("Fail the Build")
            }
       } 
    },
   "stream 4" : { 
        node ('prom-node-worker') {  
            WORKER_IP = sh(returnStdout: true, script:'echo $OPENSTACK_PUBLIC_IP').trim()
            W_HOST_IFACE = sh(returnStdout: true, script: 'echo $(ip route | grep \"^default\" | head -1 | awk \'{ print $5 }\')').trim()
            if(W_HOST_IFACE == ""){
                echo("Fail the Build")
            }
            W_HOSTNAME=sh(returnStdout: true, script: 'echo $(hostname)').trim()
            if(W_HOSTNAME == ""){
                echo("Fail the Build")
            }
        }
    },
    "stream 5": {
        node ('prom-build-host') {
            echo ("Just building in parallel with my friends.")
        }
    })
}
stage('Build Promenade Image'){
        node('prom-build-host'){
            
            checkout([$class: 'GitSCM', branches: [[name: '*/testing-framework']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/mark-burnett/promenade.git']]])
            
            dir("${WORKSPACE}/tools") {
                sh 'cat promenade-config.yaml'
                
                sh ( 'sed -i "s|quay.io/attcomdev/promenade:latest|slfletch/promenade:'+MAJOR_VERSION+'.'+MINOR_VERSION+'.'+PATCH_VERSION+'|g" promenade-config.yaml')
                sh ( 'sed -i "s|GENESIS|'+GENESIS_IP+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|MASTER_1|'+MASTER_1_IP+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|MASTER_2|'+MASTER_2_IP+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|WORKER|'+WORKER_IP+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|G_IFACE|'+G_HOST_IFACE+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|M1_IFACE|'+M1_HOST_IFACE+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|M2_IFACE|'+M2_HOST_IFACE+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|W_IFACE|'+W_HOST_IFACE+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|G_HOSTNAME|'+G_HOSTNAME+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|M1_HOSTNAME|'+M1_HOSTNAME+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|M2_HOSTNAME|'+M2_HOSTNAME+'|g" promenade-config.yaml' )
                sh ( 'sed -i "s|W_HOSTNAME|'+W_HOSTNAME+'|g" promenade-config.yaml' )
                sh 'cat promenade-config.yaml'
            }
            
            def DOCKER_BUILD_RESULT = sh(returnStatus: true, script: 'sudo docker build -t slfletch/promenade:'+ MAJOR_VERSION+'.'+MINOR_VERSION+'.'+PATCH_VERSION+' .')
            if(DOCKER_BUILD_RESULT != 0){
                error('Failed to build Promenade Image')
            }
            def DOCKER_TAG_RESULT = sh(returnStatus: true, script: 'sudo docker tag slfletch/promenade:'+ MAJOR_VERSION+'.'+MINOR_VERSION+'.'+PATCH_VERSION+' slfletch/promenade:'+ MAJOR_VERSION+'.'+MINOR_VERSION+'.'+PATCH_VERSION)
            if(DOCKER_TAG_RESULT != 0){
                error('Failed to tag Promenade Image')
            }
        } 
}
stage('Run Promenade Image'){
        node('prom-build-host'){
            dir("${WORKSPACE}/tools") {
               
                def DOCKER_RUN_RESULT = sh(returnStatus: true, script: 'sudo docker run --rm -v $(pwd):/tools slfletch/promenade:'+ MAJOR_VERSION+'.'+MINOR_VERSION+'.'+PATCH_VERSION+' promenade -v generate -c /tools/promenade-config.yaml -o /tools/generated_configs')
                if(DOCKER_RUN_RESULT != 0){
                    error('Failed to tag Promenade Image')
                }
                sh ('sudo tar -cf configs.tar generated_configs')
              
                def DOCKER_TRANSFER_RESULT = sh(returnStatus: true, script: 'scp -i /home/ubuntu/jenkins-slave-keypair.pem -o StrictHostKeyChecking=no configs.tar '+GENESIS_IP+':$HOME')
                sh ('scp -i /home/ubuntu/jenkins-slave-keypair.pem -o StrictHostKeyChecking=no configs.tar '+MASTER_1_IP+':$HOME')
                sh ('scp -i /home/ubuntu/jenkins-slave-keypair.pem -o StrictHostKeyChecking=no configs.tar '+MASTER_2_IP+':$HOME')
                sh ('scp -i /home/ubuntu/jenkins-slave-keypair.pem -o StrictHostKeyChecking=no configs.tar '+WORKER_IP+':$HOME')
               
                sh 'sudo docker login --username=slfletch --password=riley2901 '
                sh 'sudo docker push slfletch/promenade:0.1.0'
            //    sh 'curl -H \'X-JFrog-Art-Api: AKCp5Z2Nqn8uguDJHQiHHcHR6yz2XXaczzaToeFFWLHpQYEwEjLJsUy1QvXepVBe8576yrdcR\' -T configs.tar "https://12.37.173.196/artifactory/ucp-generic/promenade/${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}/configs-${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH_VERSION}.tar"'
     
            }
            
             //Run Generate with tag (major/minor/patch)
             //up.sh and yaml file (Artifactory) all at once
             //Upload to Artifactory - Tag with Jenkins major/minor/patch
             
        } 
}
node('prom-node-genesis'){
    stage('Genesis'){
        sh 'pwd'
        sh 'sudo docker login --username=slfletch --password=riley2901 '
        sh 'sudo docker pull slfletch/promenade:0.1.0'
        sh 'ls'
        sh 'pwd'
        sh 'cp /home/ubuntu/configs.tar ./'
        sh 'tar -xvf configs.tar'
        sh ('''cd generated_configs 
               chmod +x up.sh
               sudo ./up.sh $(hostname).yaml''')
       

       //Download from Artifactory
         //Run up.sh and point to yaml file arg
         //When is Genesis done?  DNS and tiller are up
    }
    stage('Validate Genesis Bootstrap'){
        sh 'pwd'
        sh 'ls -ltr'
        sh '''cd generated_configs
              chmod +x validate-bootstrap.sh
              sudo ./validate-bootstrap.sh'''
    }
}
stage('Join Masters'){
    node('prom-node-master-1') {
        sh 'pwd'
        sh 'ls'
        sh 'cp /home/ubuntu/configs.tar ./'
        sh 'tar -xvf configs.tar'
        sh ('''cd generated_configs 
               chmod +x up.sh
               ./up.sh $(hostname).yaml''')
    }
    node('prom-node-master-2') {
        sh 'cp /home/ubuntu/configs.tar ./'
        sh 'tar -xvf configs.tar'
        sh ('''cd generated_configs  
               chmod +x up.sh 
               ./up.sh $(hostname).yaml''')
    }
}
stage('Validate Masters'){
    node('prom-node-master-1') {
      
    }
    node('prom-node-master-2') {
        
    }
}
stage('Join Worker'){
    node('prom-node-worker') {
        sh 'cp /home/ubuntu/configs.tar ./'
        sh 'tar -xvf configs.tar'
        sh ('cd generated_configs /n' + 
            ' ./up.sh $(hostname).yaml')
    }
}
stage('Validate Worker'){
    node('prom-node-worker') {
        
    }
}
stage('Validate'){
    //Verify that K8s is up and running, nginx 
    //kubectl nginx (run with replicas with 4)
    //Kill DNS pods and make sure it is rescheduled
    //Additional labels in the configuration
}

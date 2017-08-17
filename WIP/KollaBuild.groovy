#!/usr/bin/env groovy

properties([
    [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
     parameters([
        string(defaultValue: 'nova glance kestone',
                description: 'components with custom images',
                name: 'COMPONENTS_LIST'),
        booleanParam(defaultValue: true,
                     description: 'Install/Re-install openstack-kolla, If your slave already have it installed you can set it to false (uncheck)',
                     name: 'INSTALL_KOLLA'),
        booleanParam(defaultValue: true,
                      description: 'Install/Re-install openstack-helm, If your slave already have it installed you can set it to false (uncheck)',
                      name: 'INSTALL_OSH')
                ]),
     [$class: 'ThrottleJobProperty', categories: [], limitOneJobWithMatchingParams: false, maxConcurrentPerNode: 0, maxConcurrentTotal: 0, paramsToUseForLimit: '', throttleEnabled: false, throttleOption: 'project'], pipelineTriggers([])
])

node('slave') {
    stage('slave precheck') {
        sh '''#!/bin/bash -xe
        sudo apt-get install -y python-pip libpython-all-dev libpython3-all-dev libffi-dev libssl-dev gcc git ntp tox ansible docker.io
        if ! id | grep -q docker; then
            sudo adduser $USER docker||:
        fi
        if [ -z "${COMPONENTS_LIST}" ];then
            echo "ERROR: COMPONENTS_LIST parameter is empty"
            exit 1
        fi
        echo ${COMPONENT_LIST}
        echo 'Precheck complete!'
        '''
    }
    stage('deploy Kolla') {
        sh '''#!/bin/bash -xe

        if ! ${INSTALL_KOLLA}; then
            echo "Skipping installation phase"
            exit 0
        fi

        echo "Kolla deployment"
        rm -rf kolla/
        git clone https://git.openstack.org/openstack/kolla -b stable/newton
        pip install kolla/
        cd kolla/
        tox -e py27
        tox -e genconfig
        '''
    }

    stage('Generate Images') {
        sh '''#!/bin/bash -xe
        cd kolla
        source .tox/py27/bin/activate
        for os_component in ${COMPONENTS_LIST}; do
            kolla-build -b ubuntu -t source ${os_component}
        done
        '''
    }

    stage('deploy OSH') {
        sh '''#!/bin/bash -xe

        if ! ${INSTALL_OSH}; then
            echo "Skipping installation phase"
            exit 0
        fi

        rm -rf openstack-helm/
        git clone https://github.com/openstack/openstack-helm.git

        export INTEGRATION=aio INTEGRATION_TYPE=basic

        cd openstack-helm

        bash tools/gate/setup_gate.sh
        '''
   }

    stage('Upgrade images') {
        sh '''#!/bin/bash -xe
        echo ${COMPONENTS_LIST}
        cd openstack-helm/
        for os_component in ${COMPONENTS_LIST}; do
             sed -i 's|docker.io/kolla|kolla|g' ${os_component}/values.yaml
            helm upgrade -f ${os_component}/values.yaml ${os_component} ${os_component}
        done
        '''
    }

    stage('Rally') {
        sh '''#!/bin/bash -xe
        '''
    }

    stage('Results') {
        sh '''#!/bin/bash -xe
        '''
    }

}

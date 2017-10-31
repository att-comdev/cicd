#!/bin/bash

set -e

PROJECT_LIST="
armada
deckhand
drydock
promenade
shipyard
"
record_fail(){
    echo -e "\nproject $1 failed:" | tee -a $failed_log
    cat ${1}.log >> $failed_log
    #Fail logged, continue building charts manually:
    helm_pkg
}

clone_gh_projects(){
    for project in $PROJECT_LIST; do
        git clone -q --depth 1 https://review.gerrithub.io/att-comdev/${project}
    done
}

helm_pkg(){
        #Some projects fail to create charts with make or don't have Makefile,
        #I want to create and upload their charts anyway:
        for i in `ls charts`; do
            helm dep up charts/$i
            helm package charts/$i
        done
}

make_charts(){
    set -xe
    for project in $PROJECT_LIST; do
        cd ${WDIR}/build/${project}
        if [ -f Makefile ]; then
            make charts &> ${project}.log || record_fail ${project}
        else
            helm_pkg
        fi
        #FIXME: Standard location for gererated charts shuld be defined,
        #armada leaves its tgz in charts/ dir, others in projects root:
        cp -v *.tgz ${WDIR}/ || cp -v charts/*.tgz ${WDIR}/
        cd ..
    done
}

WDIR=`pwd`
failed_log=${WDIR}/build/failed.log

####MAIN####
rm -rf build && mkdir build && cd build
clone_gh_projects # 2> /dev/null
git clone -q --depth 1 https://git.openstack.org/openstack/openstack-helm.git
cd openstack-helm
if [ ! ${GERRIT_REFSPEC} = "master" ]; then
    git fetch https://git.openstack.org/openstack/openstack-helm.git ${GERRIT_REFSPEC}
    git checkout FETCH_HEAD
fi
source tools/gate/vars.sh
source tools/gate/funcs/helm.sh
which helm || helm_install
helm_serve
helm_plugin_template_install ||:
make helm-toolkit
make_charts
echo "Done!"
if [ -f $failed_log ]; then
    cat $failed_log
    exit 1
fi

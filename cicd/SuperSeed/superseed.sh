#!/bin/bash

set -xe

function git_clone {
    local project_name=$1
    local local_path=$2
    local refspec=$3
    local gerrit_url='http://gerrithub.io'
    #local gerrit_url='ssh://jenkins@gerrithub.io:29418'

    if [[ "${local_path}" =~ ^[\/\.]*$ ]]; then
        echo "Bad local path '${local_path}'"
        exit 1
    fi

    if [ -z "${refspec}" ]; then
        echo "Empty refspec given"
        exit 1
    fi

    #BE careful here!:
    #rm -rf ${local_path}/*
    git clone ${gerrit_url}/${project_name} ${local_path}

    pushd ${local_path}
    if [[ "${refspec}" =~ ^refs\/ ]]; then
        git fetch ${gerrit_url}/${project_name} ${refspec}
        git checkout FETCH_HEAD
    else
        git checkout ${refspec}
    fi
    popd
}

generate_job_name(){
    JOB_NAME=$(basename `dirname $1`)
    JENKINS_FOLDER=$(dirname $1)
    echo Job name will be: $JENKINS_FOLDER / $JOB_NAME
    mkdir -p ${WORKSPACE}/jobs
    cp -av ${JENKINS_FOLDER}/seed.groovy ${WORKSPACE}/jobs/
}


######MAIN#####

git_clone ${GERRIT_PROJECT} ${WORKSPACE} ${GERRIT_REFSPEC}

LAST_2COMMITS=`git log -2 --reverse --pretty=format:%H`

#Looking for added or modified seed/Jenkins files:
MODIFIED_FILES=`git diff --name-status ${LAST_2COMMITS} | grep seed| cut -f2`
if [ -z "${MODIFIED_FILES}" ]; then
    MODIFIED_FILES=`git diff --name-status ${LAST_2COMMITS} | grep Jenkinsfile| cut -f2`
fi

#Update existing jobs
if [ ! -z "${MODIFIED_FILES}" ]; then
    for file in ${MODIFIED_FILES}; do
        generate_job_name $file
    done
else
    echo "ERROR: No Jobs found "
    exit 1
fi

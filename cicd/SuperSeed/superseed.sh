#!/bin/bash

set -xe

git_clone(){

    local project_name=$1
    local local_path=$2
    local refspec=$3
    local gerrit_url='http://gerrithub.io'
    #local gerrit_url='ssh://jenkins@gerrithub.io:29418'

    if [[ "${local_path}" =~ ^[\/\.]*$ ]]; then
        echo "ERROR: Bad local path '${local_path}'"
        exit 1
    fi

    if [ -z "${refspec}" ]; then
        echo "ERROR: Empty refspec given"
        exit 1
    fi

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

copy_seed(){

    if [ -f "${WORKSPACE}/$1" ]; then
        echo "INFO: Seed file found! $1 "

        echo SEED_PATH=$1 > ${WORKSPACE}/SEED_PATH.txt
    else
        echo "ERROR: No seed files found"
        exit 1
    fi
}


find_seed(){

    if [ -z "${SEED_PATH}" ]; then
        if [ "${GERRIT_REFSPEC}" = "origin/master" ]; then
            echo "ERROR: empty SEED_PATH parameter."
            exit 1
        fi

        echo "INFO: Looking for seed.groovy file in refspec changes..."
        LAST_2COMMITS=`git log -2 --reverse --pretty=format:%H`

        #Looking for added or modified seed.groovy files or Jenkinsfiles:
        MODIFIED_FILES=`git diff --name-status ${LAST_2COMMITS} | grep -v ^D | grep 'seed.groovy' | cut -f2`
        echo "INFO: changed seed file: $MODIFIED_FILES"
        if [ -z "${MODIFIED_FILES}" ]; then
            #No seeds?, looking for modified Jenkinsfile files:
            MODIFIED_FILES=`git diff --name-status ${LAST_2COMMITS} | grep -v ^D | grep Jenkinsfile| cut -f2`
        echo "INFO: changed Jenkinsfile: $MODIFIED_FILES"
        fi

        if [ ! -z "${MODIFIED_FILES}" ]; then
            for file in ${MODIFIED_FILES}; do
                SEED_PATH=$(dirname ${file})/seed.groovy
                copy_seed ${SEED_PATH}
            done
        fi

    else
        #SEED_PATH param is not empty:
        copy_seed ${SEED_PATH}
    fi

    #More empty space for DSL script debug information:
    set +x
    echo -e "\n=======================================\n\n\n"
}

######MAIN#####
git_clone ${GERRIT_PROJECT} ${WORKSPACE} ${GERRIT_REFSPEC}
find_seed

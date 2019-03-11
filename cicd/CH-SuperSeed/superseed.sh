#!/bin/bash

set -xe

export http_proxy=$HTTP_PROXY
export https_proxy=$http_proxy

git_clone(){

    local project_name=$1
    local local_path=$2
    local refspec=$3
    local gerrit_url=$INTERNAL_GERRIT_SSH

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

create_seed_list(){
    # Create a list of seeds from the release file
    # which is located under the src/ directory

    local release_file=$1

    if [ ! -f ${release_file} ]; then
        echo "ERROR: Release file not found!"
        exit 1
    fi

    while read -r line || [[ -n "$line" ]]; do
        line+=","
        release_list=$release_list$line
        echo "INFO: Text read from release file: $line"
    done < ${release_file}

    export RELEASE_LIST=$release_list
    echo "INFO: RELEASE_LIST is [${RELEASE_LIST}]"

}

copy_seed(){
    # Split comma separated seed list
    # copy all seed.groovy files with prefixed dir name
    # param - comma separated relative paths of seed.groovy

    # "${BUILD_NUMBER}/seed_<seeddirname>.groovy" is a hardcoded path
    # for 'Process Job DSLs' part of the job.
    # See cicd/CH-SuperSeed/seed.groovy file.

    mkdir -p ${WORKSPACE}/${BUILD_NUMBER}
    seed_list=$(echo $1 | tr "," "\n")

    for seed in $seed_list; do
        seed_file="${WORKSPACE}/${seed}"
        if [ -f ${seed_file} ]; then
            # rename the seed groovy as an unique yet readable filename
            # this allows for multiple seed in same dir
            random_string=$(head /dev/urandom | tr -dc A-Za-z | head -c 6)
            # convert '-' to '_' to avoid dsl script name error
            seed_dir=$(dirname ${seed} | awk -F '/' '{print $NF}' | tr '-' '_')
            filename_only=$(basename ${seed} | cut -d. -f1 | tr '-' '_')
            cp -a ${seed_file} ${WORKSPACE}/${BUILD_NUMBER}/seed_${seed_dir}_${filename_only}_${random_string}.groovy
        else
            # Fail the build if file doesn't exists:
            echo "ERROR: ${seed_file} not found"
            exit 1
        fi
    done
}


######MAIN#####
git_clone ${GERRIT_PROJECT} ${WORKSPACE} ${GERRIT_REFSPEC}

create_seed_list ${RELEASE_FILE_PATH}


# Skip applying the seed files for patchsets
if [[ ${GERRIT_EVENT_TYPE} == "patchset-created" ]]; then
    echo "INFO: Not applying seeds for patchsets, Seeds are applied only after merge"
    exit 0
fi

if [[ ! ${RELEASE_LIST} =~ ^tests/ ]]; then
    copy_seed ${RELEASE_LIST}
else
    echo "Not copying seed(s), because it is in tests/ directory"
fi

# Empty space for DSL script debug information:
echo -e "=================================================\n"

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
        mkdir -p ${WORKSPACE}/${BUILD_NUMBER}
        # "${BUILD_NUMBER}/seed.groovy" is a hardcoded path
        # for 'Process Job DSLs' part of the job.
        # See cicd/SuperSeed/seed.groovy file.
        cp -a ${WORKSPACE}/$1 ${WORKSPACE}/${BUILD_NUMBER}/seed.groovy
    else
        #Fail the build if file doesn't exists:
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
        MODIFIED_FILES=`git diff --name-status --no-renames ${LAST_2COMMITS} | grep -v ^D | grep 'seed.groovy' | cut -f2`
        echo "INFO: changed seed file: $MODIFIED_FILES"
        if [ -z "${MODIFIED_FILES}" ]; then
            #No seeds?, looking for modified Jenkinsfile files:
            MODIFIED_FILES=`git diff --name-status --no-renames ${LAST_2COMMITS} | grep -v ^D | grep Jenkinsfile| cut -f2`
        echo "INFO: changed Jenkinsfile: $MODIFIED_FILES"
        fi

        if [ ! -z "${MODIFIED_FILES}" ]; then
            for file in ${MODIFIED_FILES}; do
                SEED_PATH=$(dirname ${file})/seed.groovy
                export SEED_PATH=${SEED_PATH}
            done
        fi

    else
        #if SEED_PATH param is not empty, we don't need to do anything:
        echo ${SEED_PATH}
    fi
}

lint_jenkins_files(){
    #Looking for added or modified seed.groovy files or Jenkinsfiles
    LAST_2COMMITS=$(git log -2 --reverse --pretty=format:%H)
    MODIFIED_FILES=$(git diff --name-status --no-renames ${LAST_2COMMITS} | grep -v ^D | egrep "seed.groovy|Jenkinsfile" | cut -f2)

    echo "NOTICE: Jenkins linter does not check for all errors and can't be 100% trusted"
    for file in ${MODIFIED_FILES}; do
        echo "INFO: linting jenkins file ""${file}""..."
        cat "${file}" | java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar declarative-linter
    done
}

lint_whitespaces(){
    #find whitespaces at the end of lines in all files (except hidden, e.g. .git/)
    WHITESPACEDFILES=$(find . -not -path "*/\.*" -type f -exec egrep -l " +$" {} \;)
    if [[ -z "${WHITESPACEDFILES}" ]]; then
        echo "No whitespaces at the end of lines."
    else
        echo -e "Remove whitespaces at the end of lines in the following files:\n${WHITESPACEDFILES}" >&2
        exit 1
    fi
}

######MAIN#####
git_clone ${GERRIT_PROJECT} ${WORKSPACE} ${GERRIT_REFSPEC}
lint_whitespaces
lint_jenkins_files
find_seed
set +x

if [[ ! ${SEED_PATH} =~ ^tests/ ]]; then
    copy_seed ${SEED_PATH}
else
    echo "Not copying seed, because it's tests seed."
fi

#Empty space for DSL script debug information:
echo -e "=================================================\n"

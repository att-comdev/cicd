#!/bin/bash

set -xe

export http_proxy=$HTTP_PROXY
export https_proxy=$http_proxy

git_clone(){

    local project_name=$1
    local local_path=$2
    local refspec=$3

    if [[ "${GERRIT_HOST}" =~ review ]]; then
        local gerrit_url='https://review.gerrithub.io'
    else
        local gerrit_url=$INTERNAL_GERRIT_SSH
    fi

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

get_seed(){
    # recursive search of seed.groovy for a Jenkinsfile
    # Lookup in same or higher level directories

    SEED=''
    dir_path=$1
    if [ -f "${WORKSPACE}/${dir_path}/seed.groovy" ]; then
        # found seed for the Jenkinsfile
        SEED="${dir_path}/seed.groovy"
        return
    fi

    up_dir=$(dirname ${dir_path})
    if [ ${up_dir} == "." ]; then
        # reached top level dir
        echo "ERROR: seed file not found in ${dir_path}/..."
        exit 1
    fi

    # check seed in one level higher directory
    get_seed ${up_dir}
}

copy_seed(){
    # Split comma separated seed list
    # copy all seed.groovy files with prefixed dir name
    # param - comma separated relative paths of seed.groovy

    # "${BUILD_NUMBER}/seed_<seeddirname>.groovy" is a hardcoded path
    # for 'Process Job DSLs' part of the job.
    # See cicd/SuperSeed/seed.groovy file.

    mkdir -p ${WORKSPACE}/${BUILD_NUMBER}
    seed_list=$(echo $1 | tr "," "\n")

    for seed in $seed_list; do
        seed_file="${WORKSPACE}/${seed}"
        if [ -f ${seed_file} ]; then
            # suffix directory name to seed to identify multiple seeds
            # convert '-' to '_' to avoid dsl script name error
            seed_dir=$(dirname ${seed} | awk -F '/' '{print $NF}' | tr '-' '_')
            cp -a ${seed_file} ${WORKSPACE}/${BUILD_NUMBER}/seed_${seed_dir}.groovy
        else
            # Fail the build if file doesn't exists:
            echo "ERROR: ${seed_file} not found"
            exit 1
        fi
    done
}

find_seed(){

    if [ -z "${SEED_PATH}" ]; then
        if [ "${GERRIT_REFSPEC}" = "origin/master" ]; then
            echo "ERROR: empty SEED_PATH parameter."
            exit 1
        fi

        echo "INFO: Looking for seed.groovy file(s) in refspec changes..."
        LAST_2COMMITS=`git log -2 --reverse --pretty=format:%H`

        # Looking for added or modified seed.groovy files or Jenkinsfiles or only superseed.sh:
        MODIFIED_FILES=`git diff --name-status --no-renames ${LAST_2COMMITS} | grep -v ^D | egrep "seed.groovy|Jenkinsfile|superseed.sh" | cut -f2`
        echo "INFO: changed seed or Jenkinsfile(s): $MODIFIED_FILES"

        echo "INFO: Building the SEED_PATH for all seed.groovy files..."
        if [ ! -z "${MODIFIED_FILES}" ]; then
            for file in ${MODIFIED_FILES}; do

                # lookup seed.groovy
                get_seed "$(dirname ${file})"

                if [[ -z "${SEED_PATH}" ]]; then
                    # set first time
                    SEED_PATH=${SEED}
                elif [[ ! ${SEED_PATH} =~ ${SEED} ]]; then
                    # if seed not already present, append to it
                    SEED_PATH=${SEED_PATH},${SEED}
                fi
            done
        else
            # Fail the build as no seed or jenkinsfile found
            echo "ERROR: No seed files found"
            exit 1
        fi
    fi

    export SEED_PATH=$SEED_PATH
    echo "INFO: SEED_PATH is [${SEED_PATH}]"
}


lint_jenkins_files(){
    # Looking for added or modified seed.groovy files or Jenkinsfiles
    LAST_2COMMITS=$(git log -2 --reverse --pretty=format:%H)
    MODIFIED_FILES=$(git diff --name-status --no-renames ${LAST_2COMMITS} | grep -v ^D | egrep "seed.groovy|Jenkinsfile" | cut -f2)

    echo "NOTICE: Jenkins linter does not check for all errors and can't be 100% trusted"
    for file in ${MODIFIED_FILES}; do
        # JENKINS-42730: declarative-linter don't work with shared library
        # Iterate all files and skip linter checks if file contains
        # class imports from global share libraries
        grep -rq "import.*att.*" ${file} && \
            echo "INFO:[JENKINS-42730] Skipping linter for file ""${file}""..."; continue
        echo "INFO: linting file ""${file}""..."
        opts="-s ${JENKINS_CLI_URL} -auth ${JENKINS_USER}:${JENKINS_TOKEN}"
        cat "${file}" | java -jar ${JENKINS_CLI} ${opts} declarative-linter
    done
}

lint_whitespaces(){
    # find whitespaces at the end of lines in all files (except hidden, e.g. .git/)
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
    echo "Not copying seed(s), because it is in tests/ directory"
fi

# Empty space for DSL script debug information:
echo -e "=================================================\n"

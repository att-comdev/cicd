#!/bin/bash

set -xe

#Script will ignore trigger variables for now:
GERRIT_PROJECT="att-comdev/cicd"
GERRIT_URL="https://review.gerrithub.io"
COMMON_FUNCTIONS_DIR="${HOME}/CommonFunctions"

if [ ! -d "${COMMON_FUNCTIONS_DIR}/common" ]; then
    echo "Installing common functions into ${COMMON_FUNCTIONS_DIR}"
    git clone ${GERRIT_URL}/${GERRIT_PROJECT} ${COMMON_FUNCTIONS_DIR}
else
    echo "Updating common functions in ${COMMON_FUNCTIONS_DIR}"
    cd ${COMMON_FUNCTIONS_DIR}
    git pull origin master
fi

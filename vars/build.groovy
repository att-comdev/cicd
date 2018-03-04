//Use this method to build images in a repeatable fashion
def makeImages(){
   sh "sudo make images IMAGE_PREFIX=${IMAGE_PREFIX} IMAGE_NAME=\${JOB_BASE_NAME} DOCKER_REGISTRY=\${ARTF_DOCKER_URL} LABEL='org.label-schema.vcs-ref=\${IMAGE_TAG} --label org.label-schema.vcs-url=\${GERRIT_CHANGE_URL} --label org.label-schema.version=0.1.0-\${BUILD_NUMBER}' IMAGE_TAG=\${IMAGE_TAG}"
}
def lint() {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${PEGLEG_IMAGE} pegleg lint -p ${SITE_REPO} -a ${GLOBAL_REPO} -a ${SECRETS_REPO}"
}

def render(siteName) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${PEGLEG_IMAGE} pegleg site -p ${SITE_REPO} -a ${GLOBAL_REPO} -a ${SECRETS_REPO} render ${siteName} > ${siteName}.yaml"
}

def collect(siteName, outDir) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${PEGLEG_IMAGE} pegleg site -p ${SITE_REPO} -a ${GLOBAL_REPO} -a ${SECRETS_REPO} collect ${siteName} -s ${outDir}"
}
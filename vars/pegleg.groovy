def lint(String image) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${image} pegleg lint -p ${SITE_REPO} -a ${GLOBAL_REPO} -a ${SECRETS_REPO}"
}

def render(String image, String siteName) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${image} pegleg site -p ${SITE_REPO} -a ${GLOBAL_REPO} -a ${SECRETS_REPO} render ${siteName} > ${siteName}.yaml"
}

def collect(String image, String siteName) {
    sh "mkdir -p output"
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${image} pegleg site -p ${SITE_REPO} -a ${GLOBAL_REPO} -a ${SECRETS_REPO} collect ${siteName} -s output"
}
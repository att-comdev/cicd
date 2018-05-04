def lint(peglegImage, siteRepo, globalRepo, secretsRepo) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${peglegImage} pegleg lint -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo}"
}

def lintWithinContainer(siteRepo, globalRepo, secretsRepo) {
    return sh(script: "pegleg -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} lint", returnStatus:true).trim()
}

def render(peglegImage, siteRepo, globalRepo, secretsRepo, siteName) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${peglegImage} pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} render ${siteName} > ${siteName}.yaml"
}

def renderWithinContainer(siteRepo, globalRepo, secretsRepo, siteName) {
    return sh(script: "pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} render ${siteName} > ${siteName}.yaml", returnStatus: true).trim()
}

// For use when calling Docker to run/send commands to the Pegleg container
def collect(peglegImage, siteRepo, globalRepo, secretsRepo, siteName, outDir) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        ${peglegImage} pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} collect ${siteName} -s ${outDir}"
}

// For use when calling Pegleg from directly within the container
def collectWithinContainer(siteRepo, globalRepo, secretsRepo, siteName, outDir) {
    sh "pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} collect ${siteName} -s ${outDir}"
}
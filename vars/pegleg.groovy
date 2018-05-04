import att.comdev.cicd.config.conf

// For use when calling Docker to run/send commands to the Pegleg container
def lint(siteRepo, globalRepo, secretsRepo) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg -v lint -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} -x P001 -x P003"
}

// For use when calling Pegleg from directly within the container (requires "pod as a Jenkins slave" configuration to be in place)
def lintWithinContainer(siteRepo, globalRepo, secretsRepo) {
    sh "pegleg -v lint -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} -x P001 -x P003"
}

def render(siteRepo, globalRepo, secretsRepo, siteName) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} render ${siteName} > ${siteName}.yaml"
}

def renderWithinContainer(siteRepo, globalRepo, secretsRepo, siteName) {
    return sh(script: "pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} render ${siteName} > ${siteName}.yaml", returnStatus: true).trim()
}

def collect(siteRepo, globalRepo, secretsRepo, siteName) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} collect ${siteName} -s ${siteName}"
}

def collectWithinContainer(siteRepo, globalRepo, secretsRepo, siteName) {
    sh "pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} collect ${siteName} -s ${siteName}"
}
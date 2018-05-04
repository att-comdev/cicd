def lint(peglegImage, siteRepo, globalRepo, secretsRepo) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${peglegImage} pegleg lint -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo}"
}

def render(peglegImage, siteRepo, globalRepo, secretsRepo, siteName) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        ${peglegImage} pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} render ${siteName} > ${siteName}.yaml"
}

def collect(peglegImage, siteRepo, globalRepo, secretsRepo, siteName, outDir) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        ${peglegImage} pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} collect ${siteName} -s ${outDir}"
}
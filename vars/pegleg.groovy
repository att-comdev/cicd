import att.comdev.cicd.config.conf

/**
 * Execution of Pegleg "lint" against a Pegleg
 * container. NOTE: Currently setup to ignore
 * P001 & P003. See: https://github.com/openstack/airship-pegleg/blob/master/src/bin/pegleg/pegleg/engine/errorcodes.py
 * for more information on returned error codes.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global documents (must be at your PWD)
 * @param secretsRepo The folder container your security documents (must be at your PWD)
 */
def lint(siteRepo, globalRepo, secretsRepo) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg -v lint -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} -x P001 -x P003"
}

/**
 * Execution of Pegleg "lint" within a Pegleg
 * container. NOTE: Currently setup to ignore
 * P001 & P003. See: https://github.com/openstack/airship-pegleg/blob/master/src/bin/pegleg/pegleg/engine/errorcodes.py
 * for more information on returned error codes.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global documents (must be at your PWD)
 * @param secretsRepo The folder container your security documents (must be at your PWD)
 */
def lintWithinContainer(siteRepo, globalRepo, secretsRepo) {
    sh "pegleg -v lint -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} -x P001 -x P003"
}

/**
 * Execution of Pegleg "render" against a Pegleg
 * container. Redirects the output to a file, so
 * it doesn't get written to stdout.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global documents (must be at your PWD)
 * @param secretsRepo The folder container your security documents (must be at your PWD)
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def render(siteRepo, globalRepo, secretsRepo, siteName) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} render ${siteName} > ${siteName}.yaml"
}

/**
 * Execution of Pegleg "render" within a Pegleg
 * container. Redirects the output to a file, so
 * it doesn't get written to stdout.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global documents (must be at your PWD)
 * @param secretsRepo The folder container your security documents (must be at your PWD)
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def renderWithinContainer(siteRepo, globalRepo, secretsRepo, siteName) {
    return sh(script: "pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} render ${siteName} > ${siteName}.yaml", returnStatus: true).trim()
}

/**
 * Execution of Pegleg "collect" against a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global documents (must be at your PWD)
 * @param secretsRepo The folder container your security documents (must be at your PWD)
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def collect(siteRepo, globalRepo, secretsRepo, siteName) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} collect ${siteName} -s ${siteName}"
}

/**
 * Execution of Pegleg "collect" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global documents (must be at your PWD)
 * @param secretsRepo The folder container your security documents (must be at your PWD)
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def collectWithinContainer(siteRepo, globalRepo, secretsRepo, siteName) {
    sh "pegleg site -p ${siteRepo} -a ${globalRepo} -a ${secretsRepo} collect ${siteName} -s ${siteName}"
}
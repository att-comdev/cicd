import att.comdev.cicd.config.conf

/**
 * Execution of Pegleg "lint" within a Pegleg
 * container. NOTE: Currently setup to ignore
 * P001 & P003. See: https://github.com/openstack/airship-pegleg/blob/master/src/bin/pegleg/pegleg/engine/errorcodes.py
 * for more information on returned error codes.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global documents (must be at your PWD)
 * @param secretsRepo The folder container your security documents (must be at your PWD)
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def lintWithinContainer(siteRepo, globalRepo, secretsRepo, siteName) {
    sh "pegleg -v site -r ${siteRepo} -e global=${globalRepo} -e secrets=${secretsRepo} lint ${siteName} -w P001 -w P003"
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
    sh "pegleg site -r ${siteRepo} -e global=${globalRepo} -e secrets=${secretsRepo} render ${siteName} -o ${siteName}.yaml"
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
    sh "pegleg site -r ${siteRepo} -e global=${globalRepo} -e secrets=${secretsRepo} collect ${siteName} -s ${siteName}"
}

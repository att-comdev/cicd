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
def lint(siteRepo, globalRepo, username, ssh_key, projectSite) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg -v -v site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${ssh_key} lint ${projectSite} -x P001 -x P003"
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
def lintWithinContainer(siteRepo, gerritRefspec, username, ssh_key, projectSite) {
  sh "pegleg -v site -r ${siteRepo} -e global=ssh://REPO_USERNAME@gerrit.mtn5.cci.att.com:29418/aic-clcp-manifests@${gerritRefspec} -u ${username} -k ${ssh_key} lint ${projectSite} -x P001 -x P003"
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
def render(siteRepo, globalRepo, username, ssh_key, projectSite) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg site -p ${siteRepo} -e global=${globalRepo} -u ${username} -k ${ssh_key} render ${projectSite} -o ${projectSite}.yaml"
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
def renderWithinContainer(siteRepo, globalRepo, username, ssh_key, projectSite) {
    sh "pegleg site -p ${siteRepo} -e global=${globalRepo} -u ${username} -k ${ssh_key} render ${projectSite} -o ${projectSite}.yaml"
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
def collect(siteRepo, globalRepo, username, ssh_key, projectSite) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg site -p ${siteRepo} -e global=${globalRepo} -u ${username} -k ${ssh_key} collect ${projectSite} -s ${projectSite}"
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
def collectWithinContainer(siteRepo, globalRepo, username, ssh_key, projectSite) {
    sh "pegleg -v site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${ssh_key} collect ${projectSite} -s ${projectSite}"
}


/**
 * Execution of Pegleg "encrypt" against a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global documents (must be at your PWD)
 * @param secretsRepo The folder container your security documents (must be at your PWD)
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def encrypt(siteRepo, globalRepo, username, ssh_key, author, projectSite) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg.sh -v site -r ${siteRepo} -e global=${globalRepo} secrets encrypt -a ${author} ${projectSite}"
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
def encryptWithinContainer(siteRepo, globalRepo, username, ssh_key, author, projectSite) {
    sh "pegleg.sh -v site -r ${siteRepo} -e global=${globalRepo} secrets encrypt -a ${author} ${projectSite}"
}
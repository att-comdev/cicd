import att.comdev.cicd.config.conf


/**
 * Execution of Pegleg "lint" against a Pegleg
 * container. NOTE: Currently setup to ignore
 * P001 & P003. See: https://github.com/openstack/airship-pegleg/blob/master/src/bin/pegleg/pegleg/engine/errorcodes.py
 * for more information on returned error codes.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def lint(siteRepo, globalRepo, username, sshKey, siteName) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg -v -v site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} lint ${siteName} -x P001 -x P003"
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
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def lintWithinContainer(siteRepo, globalRepo, secretsRepo, siteName) {
    sh "pegleg -v site -r ${siteRepo} -e global=${globalRepo} -e secrets=${secretsRepo} lint ${siteName} -w P001 -w P003"
}

/**
 * Execution of Pegleg "lint" within a Pegleg
 * container. NOTE: Currently setup to ignore
 * P001 & P003. See: https://github.com/openstack/airship-pegleg/blob/master/src/bin/pegleg/pegleg/engine/errorcodes.py
 * for more information on returned error codes.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param gerritRefspec The refspec of the manifests repo
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def lintWithinContainer(siteRepo, globalRepo, secretsRepo, username, sshKey, siteName, type) {
  if(type == "directory"){
      lintWithinContainer(siteRepo, globalRepo, secretsRepo, siteName)
  } else {
      sh "pegleg -v site -r ${siteRepo} -e global=${globalRepo} -e secrets=${secretsRepo} -u ${username} -k ${sshKey} lint ${siteName} -x P001 -x P003"
  }
}


/**
 * Execution of Pegleg "lint" within a Pegleg
 * container. NOTE: Currently setup to ignore
 * P001 & P003. See: https://github.com/openstack/airship-pegleg/blob/master/src/bin/pegleg/pegleg/engine/errorcodes.py
 * for more information on returned error codes.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 * @param type Globalrepo and secretRepo can either be a local directory or a git repository url
 */
def lintWithinContainer(siteRepo, username, sshKey, siteName, type) {
    sh "pegleg -v site -r ${siteRepo} -u ${username} -k ${sshKey} lint ${siteName} -x P001 -x P003"
}

/**
 * Execution of Pegleg "lint" within a Pegleg
 * container. NOTE: Currently setup to ignore
 * P001 & P003. See: https://github.com/openstack/airship-pegleg/blob/master/src/bin/pegleg/pegleg/engine/errorcodes.py
 * for more information on returned error codes.
 *
 * @param siteRepo The site repo path.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def lintAndPull(tmpPath, siteRepo, username, sshKey, siteName) {
    sh "pegleg -v site -p ${tmpPath} -r ${siteRepo} -u ${username} -k ${sshKey} lint ${siteName} -w P001 -w P003"
}

/**
 * Execution of Pegleg "lint" within a Pegleg
 * container. NOTE: Currently setup to ignore
 * P001 & P003. See: https://github.com/openstack/airship-pegleg/blob/master/src/bin/pegleg/pegleg/engine/errorcodes.py
 * for more information on returned error codes.
 *
 * @param siteRepo The site repo path.
 * @param globalRepo The global repo path.
 * @param secretsRepo The secrets repo path.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def lintAndPull(tmpPath, siteRepo, globalRepo, secretsRepo, username, sshKey, siteName) {
    sh "pegleg -v site -p ${tmpPath} -r ${siteRepo} -e global={globalRepo} -e secrets={secretsRepo} -u ${username} -k ${sshKey} lint ${siteName} -w P001 -w P003"
}

/**
 * Execution of Pegleg "render" against a Pegleg
 * container. Redirects the output to a file, so
 * it doesn't get written to stdout.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def render(siteRepo, globalRepo, username, sshKey, siteName) {
    sh "sudo docker run --rm -i --net=none --workdir='/workspace' -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg site -p ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} render ${siteName} -o ${siteName}.yaml"
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
def renderWithinContainer(siteRepoPath, globalRepoPath, secretsRepoPath, siteName, output) {
    sh "pegleg site -r ${siteRepoPath} -e global=${globalRepoPath} -e secrets=${secretsRepoPath} render ${siteName} -o ${output}.yaml"
}

/**
 * Execution of Pegleg "render" within a Pegleg
 * container. Redirects the output to a file, so
 * it doesn't get written to stdout.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 * @param "directory" for local directory repositories, "url" to override yaml site-definition
 */
def renderWithinContainer(siteRepo, globalRepo, secretsRepo, username, sshKey, siteName, type) {
  if (type == "directory"){
    renderWithinContainer(siteRepo, globalRepo, secretsRepo, siteName)
  } else {
    sh "pegleg site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} render ${siteName} -o ${siteName}.yaml"
  }
}

/**
 * Execution of Pegleg "collect" against a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def collect(siteRepo, globalRepo, username, sshKey, siteName) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg site -p ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} collect ${siteName} -s ${siteName}"
}

/**
 * Execution of Pegleg "collect" within a Pegleg
 * container.
 *
 * @param siteRepoPath The folder containing your site-level documents (must be at your PWD)
 * @param globalRepoPath The folder containing your global documents (must be at your PWD)
 * @param secretsRepoPath The folder container your security documents (must be at your PWD)
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def collectWithinContainer(siteRepoPath, globalRepoPath, secretsRepoPath, siteName, output) {
    sh "pegleg -v site -r ${siteRepoPath} -e global=${globalRepoPath} -e secrets=${secretsRepoPath} collect ${siteName} -w P001 -w P003 -s ${output}"
}

/**
 * Execution of Pegleg "collect" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 * @param "directory" for local directory repositories, "url" to override yaml site-definition
 */
def collectWithinContainer(siteRepo, globalRepo, secretsRepo, username, sshKey, siteName, type) {
  if (type == "directory"){
    collectWithinContainer(siteRepo, globalRepo, secretsRepo, siteName)
  } else {
    sh "pegleg -v site -r ${siteRepo} -e global=${globalRepo} -e secrets=${secretsRepo} -u ${username} -k ${sshKey} collect ${siteName} -s ${siteName}"
  }
}

/**
 * Execution of Pegleg "encrypt" against a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def encrypt(siteRepo, globalRepo, username, sshKey, author, siteName) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg -v site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} secrets encrypt -a ${author} ${siteName}"
}

/**
 * Execution of Pegleg "encrypt" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def encryptWithinContainer(siteRepo, globalRepo, username, sshKey, author, siteName) {
    sh "pegleg -v site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} secrets encrypt -a ${author} ${siteName}"
}

/**
 * Execution of Pegleg "encrypt" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def encryptWithinContainer(siteRepo,  username, sshKey, author, siteName, peglegPassphrase, peglegSalt) {
    sh """export PEGLEG_PASSPHRASE="${peglegPassphrase}"; export PEGLEG_SALT="${peglegSalt}"; pegleg -v site -r ${siteRepo} -u ${username} -k ${sshKey} secrets encrypt -a ${author} ${siteName}"""
}

/**
 * Execution of Pegleg "encrypt" against a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def encrypt(siteRepo, author, siteName, peglegPassphrase, peglegSalt) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        -e PEGLEG_PASSPHRASE=${peglegPassphrase} -e PEGLEG_SALT=${peglegSalt} $conf.PEGLEG_IMAGE pegleg -v site -r ${siteRepo} secrets encrypt -a ${author} ${siteName}"
}

/**
 * Execution of Pegleg "encrypt" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param username The username for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def encryptWithinContainer(siteRepo, author, siteName, peglegPassphrase, peglegSalt) {
    sh """export PEGLEG_PASSPHRASE="${peglegPassphrase}"; export PEGLEG_SALT="${peglegSalt}"; pegleg -v site -r ${siteRepo} secrets encrypt -a ${author} ${siteName}"""
}

/**
 * Execution of Pegleg "generate" against a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def generate(siteRepo, globalRepo, username, sshKey, author, siteName) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg.sh -v site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} secrets generate passphrases ${siteName} -a ${author} -s /workspace"
}

/**
 * Execution of Pegleg "generate" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def generateWithinContainer(siteRepo, globalRepo, username, sshKey, author, siteName) {
    sh "pegleg.sh -v site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} secrets generate passphrases ${siteName} -a ${author} -s /workspace"
}

/**
 * Execution of Pegleg "generate-pki" against a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def generatePki(siteRepo, globalRepo, username, sshKey, author, siteName) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        $conf.PEGLEG_IMAGE pegleg -v site -r ${siteRepo} -e global=${globalRepo} -u ${username} -k ${sshKey} secrets generate-pki -a ${author} ${siteName}"
}

/**
 * Execution of Pegleg "generate-pki" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The global repository + commit/branch/tag to checkout.
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def generatePkiWithinContainer(siteRepo, globalRepo, author, siteName) {
    sh "pegleg -v site -r ${siteRepo} -e global=${globalRepo} secrets generate-pki -a ${author} ${siteName}"
}

/**
 * Execution of Pegleg "generate-pki" against a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def generatePki(siteRepo, author, siteName, peglegPassphrase, peglegSalt) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        -e PEGLEG_PASSPHRASE=${peglegPassphrase} -e PEGLEG_SALT=${peglegSalt} $conf.PEGLEG_IMAGE pegleg -v site -r ${siteRepo} secrets generate-pki -a ${author} ${siteName}"
}

/**
 * Execution of Pegleg "generate-pki" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def generatePkiWithinContainer(siteRepo, username, sshKey, author, siteName, peglegPassphrase, peglegSalt) {
    sh """export PEGLEG_PASSPHRASE="${peglegPassphrase}"; export PEGLEG_SALT="${peglegSalt}"; pegleg -v site -r ${siteRepo} -u ${username} -k ${sshKey} secrets generate-pki -a ${author} ${siteName}"""
}

/**
 * Execution of Pegleg "generate-pki" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param siteRepo The folder containing your secrets documents (must be at your PWD)
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param siteName The name of the site you're looking to render. Must match what's in your site repository's site-definition.yaml
 */
def generatePkiWithinContainer(siteRepo, username, sshKey, secretsRepo, author, siteName, peglegPassphrase, peglegSalt) {
    sh """export PEGLEG_PASSPHRASE="${peglegPassphrase}"; export PEGLEG_SALT="${peglegSalt}"; pegleg -v site -r ${siteRepo} -e secrets=${secretsRepo} -u ${username} -k ${sshKey} secrets generate-pki -a ${author} ${siteName}"""
}

/**
 * Execution of Pegleg "genesis_bundle" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param destinationDirectory The directory where the script(s) will be generated in
 * @param siteName The name of the site you're looking to generate scripts for. Must match what's in your site repository's site-definition.yaml
 */
def generateGenesis(siteRepo, username, sshKey, destinationDirectory, siteName, peglegPassphrase, peglegSalt) {
    sh "docker run --rm -i --net=none --workdir=/workspace -v \$(pwd):/workspace \
        -e PEGLEG_PASSPHRASE=${peglegPassphrase} -e PEGLEG_SALT=${peglegSalt} $conf.PEGLEG_IMAGE pegleg -v site -u ${username} -k ${sshKey} -r ${siteRepo} genesis_bundle -b ${destinationDirectory} ${siteName} --include-validators"
}

/**
 * Execution of Pegleg "genesis_bundle" within a Pegleg
 * container.
 *
 * @param siteRepoPath The folder containing your site-level documents (must be at your PWD)
 * @param destinationDirectory The directory where the script(s) will be generated in
 * @param siteName The name of the site you're looking to generate scripts for. Must match what's in your site repository's site-definition.yaml
 */
def generateGenesisWithinContainer(siteRepoPath, globalRepoPath, secretsRepoPath, destinationDirectory, siteName) {
    sh "pegleg -v site -r ${siteRepoPath} -e global=${globalRepoPath} -e secrets=${secretsRepoPath} genesis_bundle -b ${destinationDirectory} --include-validators ${siteName}"
}

/**
 * Execution of Pegleg "genesis_bundle" within a Pegleg
 * container.
 *
 * @param siteRepo The folder containing your site-level documents (must be at your PWD)
 * @param globalRepo The folder containing your global-level documents (must be at your PWD)
 * @param username The username for the service account.
 * @param sshKey The SSH key for the service account.
 * @param destinationDirectory The directory where the script(s) will be generated in
 * @param siteName The name of the site you're looking to generate scripts for. Must match what's in your site repository's site-definition.yaml
 */
def generateGenesisWithinContainer(siteRepo, globalRepo, username, sshKey, destinationDirectory, siteName, peglegPassphrase, peglegSalt) {
        if (globalRepo == null) {
                sh """export PEGLEG_PASSPHRASE="${peglegPassphrase}"; export PEGLEG_SALT="${peglegSalt}"; pegleg -v site -u ${username} -k ${sshKey} -r ${siteRepo} genesis_bundle -b ${destinationDirectory} ${siteName} --include-validators"""
        } else {
                sh """export PEGLEG_PASSPHRASE="${peglegPassphrase}"; export PEGLEG_SALT="${peglegSalt}"; pegleg -v site -u ${username} -k ${sshKey} -r ${siteRepo} -e global=${globalRepo} genesis_bundle -b ${destinationDirectory} ${siteName} --include-validators"""
        }
}

def dockerExecLint(siteRepo, username, sshKey, siteName) {
  sh """sudo docker exec -i pegleg pegleg -v site -r ${siteRepo} -u ${username} -k ${sshKey} lint ${siteName} -x P001 -x P003"""
}

def dockerExecCollect(siteRepo, username, sshKey, siteName) {
  sh """sudo docker exec -i pegleg pegleg -v site -r ${siteRepo} -u ${username} -k ${sshKey} collect ${siteName} -s ${siteName}"""
}

def dockerExecRender(siteRepo, username, sshKey, siteName) {
  sh """sudo docker exec -i pegleg pegleg -v site -r ${siteRepo} -u ${username} -k ${sshKey} render ${siteName} -o ${siteName}.yaml"""
}

def dockerExecEncrypt(peglegPassphrase, peglegSalt, siteRepo, username, sshKey, siteName) {
  sh """
  sudo docker exec -i pegleg /bin/bash -c "export PEGLEG_PASSPHRASE="${peglegPassphrase}" && export PEGLEG_SALT="${peglegSalt}"; pegleg -v site -r ${siteRepo} -u ${username} -k ${sshKey} secrets encrypt -a ${username} ${siteName}"
  """
}

def dockerExecDecrypt(peglegPassphrase, peglegSalt, siteRepo, username, sshKey, siteName, securityRepo) {
  sh """
  sudo docker exec -i pegleg /bin/bash -c "export PEGLEG_PASSPHRASE="${peglegPassphrase}" && export PEGLEG_SALT="${peglegSalt}"; pegleg -v site -r ${siteRepo} -u ${username} -k ${sshKey} secrets decrypt ${siteName} --path ${securityRepo}"
  """
}

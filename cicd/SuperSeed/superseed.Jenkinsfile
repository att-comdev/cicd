import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

node('controller') {
    changedJobSeeds = []
    errors = []
    stage('Clean workspace') {
        cleanWs()
    }
    stage('Check parameters') {
        if (SEED_PATH && RELEASE_FILE_PATH) {
            error('SEED_PATH and RELEASE_FILE_PATH parameters can not be used at the same time')
        }
        if (!GERRIT_REFSPEC) {
            error('Empty refspec given')
        }
    }
    stage('Clone git repository') {
        // old SuperSeed job was using 'origin/<branch_name>' instead of refspec when needed to checkout a latest revision from a branch
        // because it used a raw git checkout command
        // now we use gerrit SCM plugin that accepts only the name of a branch.
        // We still allow old format for compatibility but remove 'origin' before passing refspec to the plugin.
        GERRIT_REFSPEC = GERRIT_REFSPEC.replaceFirst(/^origin\//, '')

        // this logic may look odd but it's here for backward compatibility.
        if (GERRIT_HOST.toLowerCase().contains('review')) {
            gerrit.cloneToBranch("https://review.gerrithub.io/$GERRIT_PROJECT", GERRIT_REFSPEC, WORKSPACE)
        } else if(GERRIT_HOST.toLowerCase().contains('github.com')) {
            def conf = new com.att.nccicd.config.conf(env).CONF
            withCredentials([sshUserPrivateKey([credentialsId: conf.JENKINS_GITHUB_CRED_ID, keyFileVariable: 'privateKeyFilePath', usernameVariable: 'sshUsername'])]) {
                gerrit.cloneToBranch("https://${sshUsername}@${conf.GITHUB_URL}/${GITHUB_REPO_PATH_PREFIX}${GITHUB_REPO_PREFIX}${GERRIT_PROJECT}", GERRIT_REFSPEC, WORKSPACE)
            }
        } else {
            gerrit.cloneToBranch("$INTERNAL_GERRIT_SSH/$GERRIT_PROJECT", GERRIT_REFSPEC, WORKSPACE, INTERNAL_GERRIT_KEY)
        }
        projectFolder = new File(WORKSPACE)
    }
    stage('Process changed files') {
        if (RELEASE_FILE_PATH) {
            println "[Info] Using release file: ${RELEASE_FILE_PATH}"
            changedJobSeeds = new File(WORKSPACE, RELEASE_FILE_PATH)
            .readLines()
            .collect { it.replaceAll(/#.*/, '').trim() }
            .findAll { it }
            .collect { new File(WORKSPACE, it) }
        } else if (SEED_PATH) {
            println "[Info] Using seed path(s): ${SEED_PATH}"
            changedJobSeeds = SEED_PATH.split(',').collect { new File(WORKSPACE, it.trim()) }
        } else {
            println '[Info] Scanning changed files to find seeds'
            changedFiles = sh(script: 'git diff --name-only HEAD~1 HEAD --diff-filter d', returnStdout: true).readLines()
            def lintWhitespacesResults = []

            for (file in changedFiles) {
                println "Checking file $file"
                if(file.startsWith("src/") || file.startsWith("vars/")) {
                    println "Files form the shared libraries folders are not seeded. Skipping."
                    continue
                }
                seedFiles = findSeedGroovy(new File(WORKSPACE, file))
                if (seedFiles != null) {
                    if (seedFiles.size() == 0) {
                        errors.add([file, "No seed files found for \"${file}\"."])
                    }
                    else if (seedFiles.size() > 1) {
                        errors.add([file, "Too many seed files found for \"${file}\". Found files: ${seedFiles}"])
                    } else {
                        changedJobSeeds.add(seedFiles[0])
                        lintWhitespacesResults.add(lintWhitespaces(file))
                    }
                }
            }

            // filter linting results to keep errors only
            lintWhitespacesResults = lintWhitespacesResults.findAll { it[1].size() > 0 }

            if (lintWhitespacesResults.any()) {
                for (result in lintWhitespacesResults) {
                    println "[Error] Found trailing whitespaces in the file \"${result[0]}\" on the line(s): ${result[1].join(', ')}"
                }
                error('Linting failed. Please see the log for more details.')
            }
        }

        // make sure every job gets seeded just once.
        changedJobSeeds = changedJobSeeds.unique(false)
    }
    stage('Seed jobs') {
        println "[Info] Files to seed:\n${changedJobSeeds.join('\n')}"
        if (!changedJobSeeds.any()) {
            println '[Info] No changed jobs found.'
            Utils.markStageSkippedForConditional(STAGE_NAME)
        }
        else if (!RELEASE_FILE_PATH && !SEED_PATH && env.GERRIT_EVENT_TYPE != 'change-merged') {
            println '[Info] Seeding is skipped.'
            def summaryText = '<h3>Files to seed (when merged):</h3>'
            summaryText += "<b><ul><li>${changedJobSeeds.join('</li><li>')}</li></ul></b>"
            createSummary(icon: 'gear.svg', text: summaryText)
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            changedJobSeeds.each {
                try {
                    def scriptSourceCode = correctDependencyPaths(it)
                    println "[Info] Seeding \"${it}\""
                    jobDsl scriptText: scriptSourceCode,
                        removedJobAction: 'IGNORE',
                        removedViewAction: 'IGNORE',
                        removedConfigFilesAction: 'IGNORE',
                        additionalParameters: getContext(hudson.EnvVars)
                } catch (Exception e) {
                    println "error: ${e}"
                    errors.add([it.toString(), e.toString()])
                }
            }

            if (errors) {
                def summaryText = '<h3>Seeding errors</h3><ul>'
                summaryText += errors.collect { "<li><b>${it[0]}:</b> <span style='color:#AA0000'>${it[1]}</span></li>" }.join()
                summaryText += '</ul>'
                createSummary(icon: 'error.svg', text: summaryText)
                error('Error(s) occured during seeding. Check the log for more details.')
            }
        }
    }
}

@NonCPS
def lintWhitespaces(changedFile) {
    def fileResults = changedFile
                        .readLines()
                        .withIndex()
                        .findAll { value, index -> value ==~ /.*\s$/ }
                        .collect { value, index -> index + 1 }
    return [changedFile, fileResults]
}

@NonCPS
def findSeedGroovy(file) {
    // if there is a changed seed file - return as is
    if (file.name =~ /(?i).*seed.*\.groovy$/) {
        return [file]
    }

    def foundSeeds
    def fileMatcher = file.name =~ /(?i)(?:(.*)(?:\.groovy|\.jenkinsfile))|(?:.*jenkinsfile.*)/

    // Try to figure out matching 'seed' file for changed 'jenkinsfile'
    if (fileMatcher.matches()) {
        def directory = file.parentFile
        def baseFileName = fileMatcher[0][1]

        // First, try to find a file with the same name, that ends with ".seed.groovy"
        if (baseFileName) {
            foundSeeds = new FileNameFinder().getFileNames(directory.absolutePath, "${baseFileName}.seed.groovy").collect { new File(it) }
            if (foundSeeds.size()) {
                return foundSeeds
            }
        }
        while (directory && directory != new File(WORKSPACE)) {
            // Second, try to find just any seed files in the same folder
            println "[Info] Looking for a seed file in the \"$directory\""
            foundSeeds = new FileNameFinder().getFileNames(directory.absolutePath, "*seed*.groovy").collect { new File(it) }
            if (foundSeeds.size()) {
                return foundSeeds
            }
            // Third, try to find seed files in parent folders
            directory = directory.getParentFile()
        }

        // Return null if repository top directory is reached and nothing found.
        return []
    }

    // return null if changed file is not related to a job
    return null
}

@NonCPS
def correctDependencyPaths(seedFile) {
    // Normally, seed files should have correct paths for their dependencies
    // but unfortunately this is not how it was implemeted in the previous SuperSeed job
    // it was finding each file by it's name and copying it into the location
    // specified as an argument for the "evaluate".
    // In the new job this behavior is preserved as a fail safe option but if the dependency file is found
    // in the specified location - job will leave it as is.
    def seedScript = seedFile.text
    def dependencyMatcher = seedScript =~ /\s*evaluate.*?[\"\'](.*?\.groovy)[\"\']/
    if (dependencyMatcher.any()) {
        println "[Info] Checking dependencies for \"${seedFile}\""
        for (dependencyExpression in dependencyMatcher) {
            def dependencyFilePath = dependencyExpression[1]

            // replace env variables, such as WORKSPACE with their current values
            def resolvedFile = new File(getContext(hudson.EnvVars).expand(dependencyFilePath))
            if (resolvedFile.exists()) {
                println "[Info] Dependency \"$resolvedFile\" has correct path and don't have to be changed."
            } else {
                def filesWithMatchingName = new FileNameFinder().getFileNames(WORKSPACE, "**/${resolvedFile.name}")
                if (filesWithMatchingName.size() != 1) {
                    println "[Error] Unable to determine correct path for the dependency \"$dependencyFilePath\" (resolved to \"$resolvedFile\")"
                    if (filesWithMatchingName.any) {
                        println '[Error] Potential candidates: '
                        for (potentialMatch in filesWithMatchingName) {
                            println "[Error] \t$potentialMatch"
                        }
                        println '[Error] Please correct the dependency path and rerun this job.'
                    }
                } else {
                    // this is the legacy behavior: replace dependency path with the path of the found file
                    println "[Warning] Dependency \"$dependencyFilePath\"  (resolved to \"$resolvedFile\")"
                    println "[Warning] was found at \"${filesWithMatchingName.first()}\"."
                    println '[Warning] The path will be updated for this run but please consider changing dependency path to correct value in the source file.'

                    seedScript = seedScript.replace(dependencyExpression[0], dependencyExpression[0].replace(dependencyFilePath, filesWithMatchingName.first()))
                }
            }
        }
    }
    return seedScript
}

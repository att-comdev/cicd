import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

node('controller') {
    changedJobSeeds = []
    stage('Clean workspace') {
        cleanWs()
    }
    stage('Check parameters') {
        if(SEED_PATH && RELEASE_FILE_PATH) {
            error('SEED_PATH and RELEASE_FILE_PATH parameters can not be used at the same time')
        }
        if (!GERRIT_REFSPEC) {
            error('Empty refspec given')
        }
        // this logic may look odd but it's here for backward compatibility.
        if (GERRIT_HOST.contains('review')) {
            GERRIT_HOST = 'https://review.gerrithub.io'
        } else {
            GERRIT_HOST = INTERNAL_GERRIT_SSH
        }
    }
    stage('Clone git repository') {
        sh """
            git clone "${GERRIT_HOST.replaceAll(/[\/]+$/, '')}/${GERRIT_PROJECT}"
            cd "${GERRIT_PROJECT}"
            git fetch origin ${GERRIT_REFSPEC}
            git checkout FETCH_HEAD
        """
        repoPath = new File(WORKSPACE, GERRIT_PROJECT).toString()
    }
    stage('Process changed files') {
        if (RELEASE_FILE_PATH) {
            println "[Info] Using release file: ${RELEASE_FILE_PATH}"
            changedJobSeeds = new File(repoPath, RELEASE_FILE_PATH)
                .readLines()
                .collect { it.replaceAll(/#.*/, '').trim() }
                .findAll { it }
        } else if (SEED_PATH) {
            println "[Info] Using seed path(s): ${SEED_PATH}"
            changedJobSeeds = SEED_PATH.split(',')
        } else {
            println '[Info] Scanning changed files to find seeds'
            changedFiles = sh(script: "cd \"${repoPath}\" && git diff --name-only HEAD HEAD~1", returnStdout: true).readLines()

            def lintWhitespacesResults = []

            for (file in changedFiles.collect { new File(repoPath, it) }) {
                seedFiles = findSeedGroovy(file)
                if (seedFiles != null) {
                    if (seedFiles.size() == 0) {
                        error("No seed files found for \"${file}\".")
                    }
                    if (seedFiles.size() > 1) {
                        error("Too many seed files found for \"${file}\". Found files: ${seedFiles}")
                    }
                    changedJobSeeds.add(seedFiles[0])

                    lintWhitespacesResults.add(lintWhitespaces(file))
                }
            }

            // filter linting results to keep errors only
            lintWhitespacesResults = lintWhitespacesResults.findAll { it[1].size() > 0}

            if(lintWhitespacesResults.any()) {
                for(result in lintWhitespacesResults) {
                    println "[Error] Found trailing whitespaces in the file \"${result[0]}\" on the line(s): ${result[1].join(', ')}"
                }
                error("Linting failed. Please see the log for more details.")
            }
        }

        // make sure every job gets seeded just once.
        changedJobSeeds = changedJobSeeds.collect { it.toString() }.unique(false)
    }
    stage('Seed jobs') {
        println '[Info] Changed jobs:'
        println changedJobSeeds.join('\n')

        if (!RELEASE_FILE_PATH && !SEED_PATH && (!binding.hasVariable('GERRIT_EVENT_TYPE') || GERRIT_EVENT_TYPE != 'change-merged')) {
            println '[Info] Seeding is skipped.'
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else if (!changedJobSeeds.any()) {
            println '[Info] No changed jobs found.'
            Utils.markStageSkippedForConditional(STAGE_NAME)
        } else {
            println '[Info] Seeding'
            jobDsl targets: changedJobSeeds.join('\n'),
                removedJobAction: 'IGNORE',
                removedViewAction: 'IGNORE',
                removedConfigFilesAction: 'IGNORE'
        }
    }
}

@NonCPS
def lintWhitespaces(changedFile) {
    def fileResults = changedFile
                        .readLines()
                        .withIndex()
                        .findAll { value, index -> value ==~ /.*\s$/ }
                        .collect { value, index -> index }
    return [changedFile, fileResults]
}

@NonCPS
def findFiles(directory, nameRegex) {
    def results = []
    new File(directory).eachFile(groovy.io.FileType.FILES) {
        def baseFileNameMatcher = it =~ /(?i)(?:.*\/)*${nameRegex}/
        if (baseFileNameMatcher.matches()) {
            results.add(baseFileNameMatcher[0])
        }
    }
    return results
}

@NonCPS
def findSeedGroovy(file) {
    def foundSeeds
    def fileMatcher = file =~ /(?i)(.*\/)?(.*?)(?:\.)?(?:jenkinsfile)/
    // Try to figure out matching 'seed' file for changed 'jenkinsfile'
    if (fileMatcher.matches()) {
        def directory = fileMatcher[0][1]
        def baseFileName = fileMatcher[0][2]
        // First, try to find a file with the same name, that ends with ".seed.groovy"
        if (baseFileName) {
            foundSeeds = findFiles(directory, "${java.util.regex.Pattern.quote(baseFileName)}\\.seed\\.groovy")
            if (foundSeeds.size()) {
                return foundSeeds
            }
        }

        // Second, try to fing just any seed files in the same folder
        foundSeeds = findFiles(directory, '.*seed.*\\.groovy')
        if (foundSeeds.size()) {
            return foundSeeds
        }

        // Third, try to fing seed files in arent folders
        while (new File(directory) != new File(repoPath)) {
            directory = new File(directory).getParentFile().toString()
            foundSeeds = findFiles(directory, '.*seed.*\\.groovy')
            if (foundSeeds.size()) {
                return foundSeeds
            }
        }

        // Return null if top directory reached and nothing found.
        return []
    }
    // return changed seed files as is
    else if (file ==~ /(?i).*seed.*\.groovy/) {
        return [file]
    }

    // return null if changed file is not related to a job
    return null
}

import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

node('controller') {
    changedJobSeeds = []
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
        // this logic may look odd but it's here for backward compatibility.
        if (GERRIT_HOST.contains('review')) {
            gerrit.clone("https://review.gerrithub.io/$GERRIT_PROJECT", GERRIT_REFSPEC)
        } else {
            gerrit.clone("$INTERNAL_GERRIT_SSH/$GERRIT_PROJECT", GERRIT_REFSPEC, INTERNAL_GERRIT_KEY)
        }

        repoPath = new File(WORKSPACE, GERRIT_PROJECT).toString()
    }
    dir(repoPath) {
        stage('Process changed files') {
            if (RELEASE_FILE_PATH) {
                println "[Info] Using release file: ${RELEASE_FILE_PATH}"
                changedJobSeeds = new File(repoPath, RELEASE_FILE_PATH)
                .readLines()
                .collect { it.replaceAll(/#.*/, '').trim() }
                .findAll { it }
            } else if (SEED_PATH) {
                println "[Info] Using seed path(s): ${SEED_PATH}"
                changedJobSeeds = SEED_PATH.split(',').collect { it. trim() }
            } else {
                println '[Info] Scanning changed files to find seeds'
                changedFiles = sh(script: "cd \"${repoPath}\" && git diff --name-only HEAD HEAD~1", returnStdout: true).readLines()

                def lintWhitespacesResults = []

                for (file in changedFiles) {
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
                lintWhitespacesResults = lintWhitespacesResults.findAll { it[1].size() > 0 }

                if (lintWhitespacesResults.any()) {
                    for (result in lintWhitespacesResults) {
                        println "[Error] Found trailing whitespaces in the file \"${result[0]}\" on the line(s): ${result[1].join(', ')}"
                    }
                    error('Linting failed. Please see the log for more details.')
                }
            }

            // make sure every job gets seeded just once.
            changedJobSeeds = changedJobSeeds.collect { it.toString() }.unique(false)
        }
        stage('Seed jobs') {
            summaryText = ''
            println "[Info] Files to seed:\n${changedJobSeeds.join('\n')}"
            if (!changedJobSeeds.any()) {
                println '[Info] No changed jobs found.'
                Utils.markStageSkippedForConditional(STAGE_NAME)
            }
            //else if (!RELEASE_FILE_PATH && !SEED_PATH && env.GERRIT_EVENT_TYPE != 'change-merged') {
            else if (false) {
                println '[Info] Seeding is skipped.'
                summaryText = '<h3>Files to seed (when merged):</h3>'
                summaryText += "<b><ul><li>${changedJobSeeds.join('</li><li>')}</li></ul></b>"
                Utils.markStageSkippedForConditional(STAGE_NAME)
            } else {
                summaryText = '<h3>Seeding results</h3><ul>'
                isError = false
                changedJobSeeds.each {
                    try {
                        println "[Info] Seeding \"${it}\""
                        jobDsl scriptText: ("//Submitted by ${JOB_NAME} for ${env.GERRIT_CHANGE_URL ?: 'manual execution'}\n" +
                                            "//Original file: ${it}\n\n" +
                                            new File(repoPath, it).text),
                            removedJobAction: 'IGNORE',
                            removedViewAction: 'IGNORE',
                            removedConfigFilesAction: 'IGNORE'
                        summaryText += "<li><b>${it}:</b> Seeded</li>"
                    } catch (Exception e) {
                        println "error: ${e}"
                        summaryText += "<li><b>${it}:</b> <span style='color:#AA0000'>${e}</span></li>"
                        isError = true
                    }
                }

                summaryText += '</ul>'
                createSummary(icon: isError ? 'error.svg' : 'gear.svg', text: summaryText)

                if (isError) {
                    def approvalUrl = "${JENKINS_URL}scriptApproval/"
                    createSummary(icon: 'setting.svg', text: "Approve the scripts if needed at <a href=\"${approvalUrl}#footer\">${approvalUrl}</a>")
                    error('Error occured during seeding. Check the log for more details.')
                }
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
def findFiles(directory, nameRegex) {
    return directory
        .listFiles()
        .findAll { it.isFile() && it.name =~ nameRegex }
        .collect { it.name }
}

@NonCPS
def findSeedGroovy(file) {
    def foundSeeds
    def fileMatcher = file =~ /(?i)^(.*\/)*(.*?)(?:\.)?(?:jenkinsfile)$/
    // Try to figure out matching 'seed' file for changed 'jenkinsfile'
    if (fileMatcher.matches()) {
        def directory = fileMatcher[0][1]
        def baseFileName = fileMatcher[0][2]

        // First, try to find a file with the same name, that ends with ".seed.groovy"
        if (baseFileName) {
            foundSeeds = findFiles(new File(repoPath, directory), "${java.util.regex.Pattern.quote(baseFileName)}\\.seed\\.groovy")
            if (foundSeeds.size()) {
                return foundSeeds.collect { new File(directory, it).toString() }
            }
        }

        // Second, try to fing just any seed files in the same folder
        foundSeeds = findFiles(new File(repoPath, directory), '.*seed.*\\.groovy')
        if (foundSeeds.size()) {
            return foundSeeds.collect { new File(directory, it).toString() }
        }

        // Third, try to find seed files in parent folders
        while (directory != null) {
            directory = new File(directory).getParentFile().toString()
            foundSeeds = findFiles(new File(repoPath, directory), '.*seed.*\\.groovy')
            if (foundSeeds.size()) {
                return foundSeeds.collect { new File(directory, it).toString() }
            }
        }

        // Return null if repository top directory is reached and nothing found.
        return []
    }
    // return changed seed files as is
    else if (file =~ /(?i)seed[^\/]*\.groovy$/) {
        return [file]
    }

    // return null if changed file is not related to a job
    return null
}

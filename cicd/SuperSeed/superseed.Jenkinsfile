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
            gerrit.cloneToBranch("https://review.gerrithub.io/$GERRIT_PROJECT", GERRIT_REFSPEC, WORKSPACE)
        } else {
            gerrit.cloneToBranch("$INTERNAL_GERRIT_SSH/$GERRIT_PROJECT", GERRIT_REFSPEC, WORKSPACE, INTERNAL_GERRIT_KEY)
        }
        projectFolder = new File(WORKSPACE)
    }
    stage('Process changed files') {
        if (RELEASE_FILE_PATH) {
            println "[Info] Using release file: ${RELEASE_FILE_PATH}"
            changedJobSeeds = new File(projectFolder, RELEASE_FILE_PATH)
            .readLines()
            .collect { it.replaceAll(/#.*/, '').trim() }
            .findAll { it }
            .collect { new File(projectFolder, it) }
        } else if (SEED_PATH) {
            println "[Info] Using seed path(s): ${SEED_PATH}"
            changedJobSeeds = SEED_PATH.split(',').collect { new File(projectFolder, it.trim()) }
        } else {
            println '[Info] Scanning changed files to find seeds'
            dir(projectFolder.toString()) {
                changedFiles = sh(script: "git diff --name-only HEAD HEAD~1", returnStdout: true).readLines()
            }
            def lintWhitespacesResults = []

            for (file in changedFiles) {
                println "Checking file $file"
                seedFiles = findSeedGroovy(new File(projectFolder, file))
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
            def errors = []
            changedJobSeeds.each {
                try {
                    // Normally, seed files should have correct paths for their dependencies
                    // but unfortunately this is not how it was implemeted in the previous SuperSeed job
                    // it was finding each file by it's name and copying it into the location
                    // specified as an argument for the "evaluate".
                    // In the new job this behavior is preserved as a fail safe option but if the dependency file is found
                    // in the specified location - job will leave it as is.
                    dependencyMatcher = it.text =~ /evaluate\s*\(?\s*[\"\'](.*?\.groovy)[\"\']\s*\)?/
                    if(dependencyMatcher.any()) {
                        println "[Info] Checkinjg dependencies for \"${it}\""
                        for(dependencyExpression in dependencyMatcher) {
                            def dependencyFilePath = dependencyExpression[1]

                            // replace env variables, such as WORKSPACE with their current values
                            def engine = new groovy.text.SimpleTemplateEngine()
                            def resolvedFile = new File(engine.createTemplate(dependencyFilePath).make(env).toString())
                            if(resolvedFile.exists) {
                                println "[Info] Dependency \"$resolvedFile\" has correct path and don't have to be changed."
                            } else {
                                def filesWithMatchingName = findFiles(WORKSPACE, java.util.regex.Pattern.quote(resolvedFile.name))
                                if(filesWithMatchingName.size() != 1) {
                                    println "[Error] Unable to determine correct path for the dependency \"$dependencyFilePath\" (resolved to \"$resolvedFile\")"
                                    if(filesWithMatchingName.any) {
                                        println "[Error] Potential candidates: "
                                        for(potentialMatch in filesWithMatchingName) {
                                            println "[Error] \t$potentialMatch"
                                        }
                                        println "[Error] Please correct the dependency path and rerun this job."
                                    }
                                } else {
                                    // this is the legacy behavior: replace dependency path with the path of the found file
                                    println "[Warning] Dependency \"$dependencyFilePath\"  (resolved to \"$resolvedFile\")"
                                    println "[Warning] was found at \"${filesWithMatchingName.first()}\"."
                                    println "[Warning] The path will be updated for this run but please consider changing dependency path to correct value in the source file."
                                    it.text = it.text.replace(dependencyExpression.toString().replace(dependencyFilePath, filesWithMatchingName.first()))
                                }
                            }
                        }
                    }

                    println "[Info] Seeding \"${it}\""
                    dir(projectFolder.toString()) {
                        jobDsl scriptText: it.text,
                            removedJobAction: 'IGNORE',
                            removedViewAction: 'IGNORE',
                            removedConfigFilesAction: 'IGNORE',
                            additionalParameters: env
                    }
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
def findFiles(directory, nameRegex) {
    return directory
        .listFiles()
        .findAll { it.isFile() && it.name =~ nameRegex }
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
            foundSeeds = findFiles(new File(directory), "${java.util.regex.Pattern.quote(baseFileName)}\\.seed\\.groovy")
            if (foundSeeds.size()) {
                return foundSeeds
            }
        }

        // Second, try to find just any seed files in the same folder
        foundSeeds = findFiles(new File(directory), '.*seed.*\\.groovy')
        if (foundSeeds.size()) {
            return foundSeeds
        }

        // Third, try to find seed files in parent folders
        def parentFolder = new File(directory).getParentFile()
        while (parentFolder && parentFolder != new File(WORKSPACE)) {
            foundSeeds = findFiles(parentFolder, '.*seed.*\\.groovy')
            if (foundSeeds.size()) {
                return foundSeeds
            }
            parentFolder = new File(directory).getParentFile()
        }

        // Return null if repository top directory is reached and nothing found.
        return []
    }
    // if there is a changed seed file - return as is
    else if (file =~ /(?i)seed[^\/]*\.groovy$/) {
        return [file]
    }

    // return null if changed file is not related to a job
    return null
}

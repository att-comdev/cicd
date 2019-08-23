pipelineJob("code-review") {
    description("This job is for python applications to run pep8, bandit, unit tests, and code coverage")
    logRotator{
        daysToKeep(90)
    }
    parameters {
        stringParam {
            defaultValue("")
            description('Internal project name for manual build.')
            name ('GERRIT_PROJECT')
            trim(true)
        }
        stringParam {
            defaultValue("")
            description('Reference for manual build of internal project.\n\n' +
                        'Branch or gerrit refspec is supported.')
            name ('GERRIT_REFSPEC')
            trim(true)
        }
        stringParam {
            defaultValue("")
            description('Branch for manual build of internal project.\n\n')
            name ('GERRIT_BRANCH')
            trim(true)
        }
    }
    triggers {
        gerritTrigger {
            gerritProjects {
                gerritProject {
                    compareType('REG_EXP')
                    pattern("^aic-aqa-.*")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(false)
                    excludeTrivialRebase(false)
                    excludeNoCodeChange(false)
                }
                changeMerged()
                commentAddedContains {
                    commentAddedCommentContains('^recheck\$')
                }
            }
        }
        definition {
            cps {
                script(readFileFromWorkspace("code-review/Jenkinsfile"))
                sandbox()
            }
        }
    }
}

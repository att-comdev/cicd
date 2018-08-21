pipelineJob("code-review") {
    description("This job is for python applications to run pep8, bandit, unit tests, and code coverage")
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
                gerritProject {
                    compareType('REG_EXP')
                    pattern("^mos-(?!(requirements|build|tempest|etc)).*")
                    branches {
                        branch {
                            compareType("ANT")
                            pattern("master")
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
                    commentAddedCommentContains('recheck')
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

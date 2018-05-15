pipelineJob("code-review") {
    description("This job is for python applications to run pep8, bandit, unit tests, and code coverage")
    triggers {
        gerritTrigger {
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('REG_EXP')
                    pattern("^att-comdev/(?!(cicd|docker|maas|treasuremap|ucp-integration)).*")
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

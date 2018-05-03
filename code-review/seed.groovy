pipelineJob("code-review") {
    triggers {
        gerritTrigger {
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('REG_EXP')
                    pattern("^att-comdev/(?!(cicd|docker)).*")
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

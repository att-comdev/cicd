
folder("images")
folder("images/att-comdev")
folder("images/att-comdev/porthole")
pipelineJob("images/att-comdev/porthole/calicoctl-utility") {
    logRotator{
        daysToKeep(90)
    }
    parameters {
        stringParam {
        defaultValue("")
            description('Only for manual builds')
            name ('GERRIT_PATCHSET_REVISION')
        }
        stringParam {
            defaultValue("")
            description('Only for manual builds')
            name ('GERRIT_NEWREV')
        }
        stringParam {
            defaultValue("")
            description('Only for manual builds')
            name ('GERRIT_CHANGE_URL')
        }
        stringParam {
            defaultValue("")
            description('Only for manual builds')
            name ('GERRIT_CHANGE_EVENT')
        }
        stringParam {
            defaultValue("")
            description('Only for manual builds')
            name ('GERRIT_REFSPEC')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {
        gerritTrigger {
            silentMode(true)
            serverName('')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("att-comdev/porthole")
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
                changeMerged()
                commentAddedContains {
                    commentAddedCommentContains('recheck')
                }
            }
       }

       definition {
           cps {
               script(readFileFromWorkspace("images/att-comdev/porthole/calicoctl-utility/Jenkinsfile"))
               sandbox(false)
           }
       }
    }
}
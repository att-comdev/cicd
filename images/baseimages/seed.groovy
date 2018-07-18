folder("images/baseimages")
folder("images/baseimages/master")
folder("images/baseimages/patchset")

pipelineJob("baseimages/buildall") {
        parameters {
            stringParam {
                defaultValue("0.9.0")
                description('Put RC version here')
                name('VERSION')
            }
        }
        configure {
            node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                durationName 'hour'
                count '10'
            }
        }
        triggers {
            // gerritTrigger {
            //     silentMode(true)
            //     serverName('ATT-airship-CI')
            //     gerritProjects {
            //         gerritProject {
            //             compareType('PLAIN')
            //             pattern("att-comdev/baseimage")
            //             branches {
            //                 branch {
            //                     compareType("ANT")
            //                     pattern("**")
            //                 }
            //             }
            //             disableStrictForbiddenFileVerification(false)
            //         }
            //     }
            //     triggerOnEvents {
            //         changeMerged()
            //         commentAddedContains {
            //             commentAddedCommentContains('recheck')
            //         }
            //     }
            // }

           definition {
               cps {
                   script(readFileFromWorkspace("images/baseimages/JenkinsfileMaster"))
                   sandbox()
               }
           }
        }
    }


JOB_FOLDER = 'images/calico'
folder(JOB_FOLDER)

pipelineJob("${JOB_FOLDER}/calico") {

    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '3'
        }
    }

    // triggers {
    //     gerritTrigger {
    //         serverName('Gerrithub-jenkins')
    //         gerritProjects {
    //             gerritProject {
    //                 compareType('PLAIN')
    //                 pattern("att-comdev/deckhand")
    //                 branches {
    //                     branch {
    //                         compareType("ANT")
    //                         pattern("**")
    //                     }
    //                 }
    //                 disableStrictForbiddenFileVerification(false)
    //             }
    //         }
    //         triggerOnEvents {
    //             patchsetCreated {
    //                excludeDrafts(false)
    //                excludeTrivialRebase(false)
    //                excludeNoCodeChange(false)
    //             }
    //             changeMerged()
    //         }
    //     }
    // }

    definition {
        cps {
            script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
            sandbox()
        }
    }
}


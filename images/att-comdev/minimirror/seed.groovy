JOB_FOLDER="images/att-comdev/minimirror"
folder("images/att-comdev")
folder("images/att-comdev/minimirror")
pipelineJob("${JOB_FOLDER}/minimirror") {
    logRotator{
        daysToKeep(90)
    }
    configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '3'
                }
    }
    parameters {
        stringParam {
            name ('MINIMIRROR_PROJECT')
            defaultValue('openstack/openstack-helm-images')
            description('mini-mirror project')
        }
        stringParam {
            name ('CLCP_MANIFESTS')
            defaultValue('refs/changes/47/53947/48')
            description('Gerrit refspec')
        }
    }
    triggers {
        gerritTrigger {
            silentMode(false)
            serverName('Any Server')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("openstack/openstack-helm-images")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('ANT')
                            pattern("mini-mirror/**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
                gerritProject {
                    compareType('PLAIN')
                    pattern("aic-clcp-manifests")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**")
                        }
                    }
                    filePaths {
                        filePath {
                            compareType('ANT')
                            pattern("tools/mini*/**")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(true)
                    excludeTrivialRebase(false)
                    excludeNoCodeChange(true)
                }
                changeMerged()
                commentAddedContains {
                   commentAddedCommentContains('recheck')
                }
            }
        }
    }
    definition {
        cps {
          script(readFileFromWorkspace("${JOB_FOLDER}/Jenkinsfile"))
            sandbox(false)
        }
    }
}


folder("images/baseimages")

pipelineJob("images/baseimages/buildall") {
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

       definition {
           cps {
               // TBD.
               // Need to invoke the other job in a specific order
               sandbox()
           }
       }
    }
}

pipelineJob("images/baseimages/master/docker-brew-ubuntu-core-xenial") {
    parameters {
        stringParam {
            defaultValue("ncbaseimages/ubuntu:xenial")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("scratch")
            description('Name of the Image to publish')
            name('BASE_IMAGE')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {

       definition {
           cps {
               script(readFileFromWorkspace("images/baseimages/JenkinsfileMaster"))
               sandbox()
           }
       }
    }
}

pipelineJob("images/baseimages/master/buildpack-deps-xenial-curl") {
    parameters {
        stringParam {
            defaultValue("ncbaseimages/buildpack-deps:xenial-curl")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("ncbaseimages/ubuntu:xenial")
            description('Name of the Image to publish')
            name('BASE_IMAGE')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {

       definition {
           cps {
               script(readFileFromWorkspace("images/baseimages/JenkinsfileMaster"))
               sandbox()
           }
       }
    }
}

pipelineJob("images/baseimages/master/buildpack-deps-xenial-scm") {
    parameters {
        stringParam {
            defaultValue("ncbaseimages/buildpack-deps:xenial-scm")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("ncbaseimages/buildpack-deps:xenial-curl")
            description('Name of the Image to publish')
            name('BASE_IMAGE')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {

       definition {
           cps {
               script(readFileFromWorkspace("images/baseimages/JenkinsfileMaster"))
               sandbox()
           }
       }
    }
}

pipelineJob("images/baseimages/master/buildpack-deps-xenial") {
    parameters {
        stringParam {
            defaultValue("ncbaseimages/buildpack-deps:xenial")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("ncbaseimages/buildpack-deps:xenial-scm")
            description('Name of the Image to publish')
            name('BASE_IMAGE')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {

       definition {
           cps {
               script(readFileFromWorkspace("images/baseimages/JenkinsfileMaster"))
               sandbox()
           }
       }
    }
}

pipelineJob("images/baseimages/master/python-3.5-xenial") {
    parameters {
        stringParam {
            defaultValue("ncbaseimages/python:3.5")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("ncbaseimages/buildpack-deps:xenial")
            description('Name of the Image to publish')
            name('BASE_IMAGE')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {

       definition {
           cps {
               script(readFileFromWorkspace("images/baseimages/JenkinsfileMaster"))
               sandbox()
           }
       }
    }
}

pipelineJob("images/baseimages/master/python-3.6-xenial") {
    parameters {
        stringParam {
            defaultValue("ncbaseimages/python:3.6")
            description('Name of the Image to publish')
            name('TARGET_IMAGE')
        }
        stringParam {
            defaultValue("ncbaseimages/buildpack-deps:xenial")
            description('Name of the Image to publish')
            name('BASE_IMAGE')
        }
    }
    configure {
        node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '10'
        }
    }
    triggers {

       definition {
           cps {
               script(readFileFromWorkspace("images/baseimages/JenkinsfileMaster"))
               sandbox()
           }
       }
    }
}








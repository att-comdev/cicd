JOB_BASE='images/openstack/loci/mos-review'
folder("${JOB_BASE}")

REQ_PROJECT_NAME = 'mos-requirements'
PROJECT_LIST = [
    REQ_PROJECT_NAME, 'mos-keystone', 'mos-heat', 'mos-glance',
    'mos-cinder', 'mos-neutron', 'mos-nova', 'mos-horizon',
]
DEPENDENCY_PROJECT_LIST = [
    'mos-keystoneclient', 'mos-neutronclient', 'mos-novaclient',
    'openstack/tap-as-a-service', 'openstack/tap-as-a-service-dashboard']

BRANCH = 'master'

def getRefParamName(project) {
    project.split('/')[-1].toUpperCase().replace('-', '_') + "_REF"
}

pipelineJob("${JOB_BASE}/GenericPipeline") {
    logRotator{
        daysToKeep(90)
    }
    // limit surge of patchsets
    configure { node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
            durationName 'hour'
            count '333'
        }
    }
    parameters {
        stringParam {
            defaultValue(BRANCH)
            description('Default branch for manual build.\n\n' +
                        'Currently master is supported.')
            name('BRANCH')
        }
        stringParam {
            defaultValue('')
            description('Url to requirements loci image.\n\n'   +
                        'Can not be used with '                 +
                        "${getRefParamName(REQ_PROJECT_NAME)} " +
                        'and DEPENDENCY_LIST')
            name ('REQUIREMENTS_LOCI_IMAGE')
        }
        stringParam {
            description('List of dependency components with refs separated ' +
                        'by space to override in upper-constraints.txt \n\n' +
                        'E.g.: mos-neutronclient:refs/changes/82/47482/9 '   +
                        'mos-keystoneclient:refs/changes/57/38657/4\n\n'     +
                        'Each component must be present in '                 +
                        'upper-constraints.txt and point to repository url.')
            defaultValue('')
            name('DEPENDENCY_LIST')
        }
        stringParam {
            defaultValue("manual")
            description('Type of event that triggered job.\n\n'     +
                        'Only "manual" is supported for manually ' +
                        'triggered jobs')
            name("EVENT_TYPE")
        }
        PROJECT_LIST.each {
            paramName = getRefParamName(it)
            projectName = "${it}"
            stringParam {
                description("Reference for ${projectName} build.\n\n"  +
                            'Branch, gerrit refspec and revision are ' +
                            'supported.')
                defaultValue('')
                name(paramName)
            }
        }
    }
    environmentVariables(
        "JOB_BASE": JOB_BASE,
        "PROJECT_LIST": PROJECT_LIST.join(" "),
        "REQ_PROJECT_NAME": REQ_PROJECT_NAME,
    )
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileGeneric"))
            sandbox(false)
        }
    }
}

pipelineJob("${JOB_BASE}/GenericImageBuildPipeline") {
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileGenericImageBuild"))
            sandbox(false)
        }
    }
    parameters {
        stringParam {
            defaultValue('')
            description('Project name')
            name('PROJECT_NAME')
        }
        stringParam {
            defaultValue('')
            description('Branch or gerrit refspec is supported.')
            name ('PROJECT_REF')
        }
        stringParam {
            defaultValue(BRANCH)
            description('Currently master is supported.')
            name ('PROJECT_BRANCH')
        }
        stringParam {
            defaultValue('')
            description('Supported image build types for\n' +
                        'mos-neutron: neutron-sriov\n'      +
                        'mos-nova: nova-1804')
            name ('BUILD_TYPE')
        }
        stringParam {
            defaultValue('')
            description('Url to requirements loci image.\n\n' +
                        'If empty, default one is used.')
            name ('REQUIREMENTS_LOCI_IMAGE')
        }
        stringParam {
            defaultValue("manual")
            description('Type of event that triggered job.\n\n'     +
                        'Only "manual" is supported for manually ' +
                        'triggered jobs')
            name("EVENT_TYPE")
        }
    }
    environmentVariables(
        "RESTRICT_EVENT_TYPE": false,
    )
}

pipelineJob("${JOB_BASE}/TestDeploymentPipeline") {
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileTestDeployment"))
            sandbox(false)
        }
    }
    parameters {
        stringParam {
            defaultValue('')
            description('')
            name('OVERRIDE_IMAGES')
        }
    }
}


pipelineJob("${JOB_BASE}/CodeReviewPipeline") {
    parameters {
        stringParam {
            defaultValue('mos-neutron')
            description('')
            name('GERRIT_PROJECT')
        }
        stringParam {
            defaultValue('master')
            description('')
            name('GERRIT_REFSPEC')
        }
        stringParam {
            defaultValue('patchset-created')
            description('')
            name('GERRIT_EVENT_TYPE')
        }
    }
    environmentVariables(
        "JOB_BASE":     JOB_BASE,
        "PROJECT_LIST": PROJECT_LIST.join(" "),
    )
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileCodeReview"))
            sandbox(false)
        }
    }
    triggers {
        gerritTrigger {
            serverName('mtn5-gerrit')
            gerritProjects {
                gerritProject {
                    compareType('REG_EXP')
                    pattern((PROJECT_LIST + DEPENDENCY_PROJECT_LIST).join('|'))
                    branches {
                        branch {
                            compareType("ANT")
                            pattern(BRANCH)
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
                    commentAddedCommentContains('^recheck')
                }
            }
            silentMode(true)
        }
    }
}

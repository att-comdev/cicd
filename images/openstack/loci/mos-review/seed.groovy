import groovy.json.JsonOutput

JOB_BASE='images/openstack/loci/mos-review'
folder("${JOB_BASE}")
LOCI_BUILD_SLAVE_LABEL = 'loci_generic_review'
RETRY_COUNT = 3

REQ_PROJECT_NAME = 'mos-requirements'
PROJECT_MAP = [
    "${REQ_PROJECT_NAME}": [],
    'mos-keystone':        [],
    'mos-heat':            [],
    'mos-glance':          [],
    'mos-cinder':          [],
    'mos-horizon':         [],
    'mos-neutron':         ['neutron', 'neutron-sriov'],
    'mos-nova':            ['nova', 'nova-1804'],
]
DEPENDENCY_PROJECT_LIST = [
    'mos-keystoneclient',
    'mos-neutronclient',
    'mos-novaclient',
    'mos-glance-store',
    'openstack/tap-as-a-service',
    'openstack/tap-as-a-service-dashboard',
]

BRANCH = 'master'

DEPENDENCY_LIST_PARAM_DESC = (
    'List of dependency components with refs separated ' +
    'by space to override in upper-constraints.txt \n\n' +
    'E.g.: mos-neutronclient:refs/changes/82/47482/9 '   +
    'mos-keystoneclient:refs/changes/57/38657/4\n\n'     +
    'Each component must be present in '                 +
    'upper-constraints.txt and point to repository url.'
)

// used for automatic mos-requirements change creation on change-merged event
// for dependency component
UPDATE_TOPIC = 'update_dependency'

def getRefParamName(project) {
    project.split('/')[-1].toUpperCase().replace('-', '_') + "_REF"
}

pipelineJob("${JOB_BASE}/GenericPipeline") {
    logRotator{
        daysToKeep(90)
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
            description(DEPENDENCY_LIST_PARAM_DESC)
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
        PROJECT_MAP.keySet().each {
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
    properties {
        disableResume()
    }
    environmentVariables(
        "JOB_BASE":               JOB_BASE,
        "PROJECT_MAP":            JsonOutput.toJson(PROJECT_MAP),
        "REQ_PROJECT_NAME":       REQ_PROJECT_NAME,
        "RETRY_COUNT":            RETRY_COUNT,
    )
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileGeneric"))
            sandbox(false)
        }
    }
}

PROJECT_MAP.each { projectName, buildTypes ->
    if (!buildTypes) {
        buildTypes = [projectName.split('-')[-1]]
    }
    buildTypes.each { buildType ->
        pipelineJob("${JOB_BASE}/mos-${buildType}") {
            properties {
                disableResume()
            }
            definition {
                cps {
                    script(readFileFromWorkspace(
                               "${JOB_BASE}/JenkinsfileGenericImageBuild")
                    )
                    sandbox(false)
                }
            }
            parameters {
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
                    defaultValue("manual")
                    description('Type of event that triggered job.\n\n'     +
                                'Only "manual" is supported for manually ' +
                                'triggered jobs')
                    name("EVENT_TYPE")
                }
                if (projectName != REQ_PROJECT_NAME) {
                    stringParam {
                        defaultValue('')
                        description('Url to requirements loci image.\n\n' +
                                    'If empty, default one is used.')
                        name ('REQUIREMENTS_LOCI_IMAGE')
                    }
                } else {
                    stringParam {
                        description(DEPENDENCY_LIST_PARAM_DESC)
                        defaultValue('')
                        name('DEPENDENCY_LIST')
                    }
                }
            }
            environmentVariables(
                "RESTRICT_EVENT_TYPE": false,
                "UPDATE_TOPIC":        UPDATE_TOPIC,
                "PROJECT_NAME":        projectName,
                "BUILD_TYPE":          buildType,
                "LOCI_BUILD_SLAVE_LABEL": LOCI_BUILD_SLAVE_LABEL,
            )
        }
    }
}

pipelineJob("${JOB_BASE}/TestDeploymentPipeline") {
    definition {
        cps {
            script(readFileFromWorkspace(
                      "${JOB_BASE}/JenkinsfileTestDeployment")
            )
            sandbox(false)
        }
    }
    properties {
        disableResume()
    }
    environmentVariables(
        "LOCI_BUILD_SLAVE_LABEL": LOCI_BUILD_SLAVE_LABEL,
    )
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
        "PROJECT_LIST": JsonOutput.toJson(PROJECT_MAP.keySet()),
    )
    properties {
        disableResume()
    }
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
                    pattern((PROJECT_MAP.keySet() +
                             DEPENDENCY_PROJECT_LIST).join('|'))
                    branches {
                        branch {
                            compareType("ANT")
                            pattern(BRANCH)
                        }
                    }
                    topics {
                        topic {
                            compareType('REG_EXP')
                            pattern("^(?!${UPDATE_TOPIC}).*\$")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            gerritBuildStartedVerifiedValue(-1)
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
            silentMode(false)
        }
    }
}


pipelineJob("${JOB_BASE}/ReleaseNightlyPipeline") {
    environmentVariables(
        "JOB_BASE":                JOB_BASE,
        "PROJECT_MAP":             JsonOutput.toJson(PROJECT_MAP),
        "DEPENDENCY_PROJECT_LIST": JsonOutput.toJson(DEPENDENCY_PROJECT_LIST),
    )
    properties {
        disableResume()
    }
    definition {
        cps {
            script(readFileFromWorkspace(
                       "${JOB_BASE}/JenkinsfileReleaseNightly")
            )
            sandbox(false)
        }
    }
}

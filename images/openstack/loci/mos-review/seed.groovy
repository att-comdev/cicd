import groovy.json.JsonOutput

JOB_BASE='images/openstack/loci/mos-review'
folder("${JOB_BASE}")
LOCI_BUILD_SLAVE_LABEL = 'loci_generic_review'
RETRY_COUNT = 2
NET_RETRY_COUNT = 5
RELEASES = ['ocata', 'stein']

MIRRORS_PREFIX = 'mirrors/mos/'
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
        choiceParam {
            choices(RELEASES.join("\n"))
            description("Supported releases: ${RELEASES.join(', ')}")
            name('RELEASE')
        }
        stringParam {
            defaultValue('')
            description('Url to requirements loci image.\n\n'   +
                        'Can not be used with '                 +
                        "${getRefParamName(REQ_PROJECT_NAME)} " +
                        'and DEPENDENCY_LIST')
            name ('REQUIREMENTS_LOCI_IMAGE')
            trim(true)
        }
        stringParam {
            description(DEPENDENCY_LIST_PARAM_DESC)
            defaultValue('')
            name('DEPENDENCY_LIST')
            trim(true)
        }
        stringParam {
            defaultValue("manual")
            description('Type of event that triggered job.\n\n'    +
                        'Only "manual" is supported for manually ' +
                        'triggered jobs')
            name("EVENT_TYPE")
            trim(true)
        }
        booleanParam {
            defaultValue(false)
            description('RUN AIO deployment against image(s)')
            name('RUN_DEPLOYMENT')
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
                trim(true)
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
                    trim(true)
                }
                choiceParam {
                    choices(RELEASES.join("\n"))
                    description("Supported releases: ${RELEASES.join(', ')}")
                    name('RELEASE')
                }
                stringParam {
                    defaultValue(DEFAULT_RELEASE)
                    description("Supported releases: ${RELEASES.join(' ,')}.")
                    name ('RELEASE')
                    trim(true)
                }
                stringParam {
                    defaultValue("manual")
                    description('Type of event that triggered job.\n\n'    +
                                'Only "manual" is supported for manually ' +
                                'triggered jobs')
                    name("EVENT_TYPE")
                    trim(true)
                }
                booleanParam {
                    defaultValue(false)
                    description('Add custom debian repository, specified in ' +
                                'OVS_REPO config parameter, to base image')
                    name("CUSTOM_OVS")
                }
                if (projectName != REQ_PROJECT_NAME) {
                    stringParam {
                        defaultValue('')
                        description('Url to requirements loci image.\n\n' +
                                    'If empty, default one is used.')
                        name ('REQUIREMENTS_LOCI_IMAGE')
                        trim(true)
                    }
                } else {
                    stringParam {
                        description(DEPENDENCY_LIST_PARAM_DESC)
                        defaultValue('')
                        name('DEPENDENCY_LIST')
                        trim(true)
                    }
                }
            }
            environmentVariables(
                "RESTRICT_EVENT_TYPE":    false,
                "UPDATE_TOPIC":           UPDATE_TOPIC,
                "PROJECT_NAME":           projectName,
                "BUILD_TYPE":             buildType,
                "NET_RETRY_COUNT":        NET_RETRY_COUNT,
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
            trim(true)
        }
        choiceParam {
            choices(RELEASES.join("\n"))
            description("Supported releases: ${RELEASES.join(', ')}")
            name('RELEASE')
        }
    }
}


pipelineJob("${JOB_BASE}/CodeReviewPipeline") {
    parameters {
        stringParam {
            description('')
            name('GERRIT_PROJECT')
            trim(true)
        }
        stringParam {
            defaultValue('master')
            description('')
            name('GERRIT_REFSPEC')
            trim(true)
        }
        stringParam {
            defaultValue('patchset-created')
            description('')
            name('GERRIT_EVENT_TYPE')
            trim(true)
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


RELEASES.each {
    pipelineJob("${JOB_BASE}/ReleaseNightlyPipeline${it.capitalize()}") {
        environmentVariables(
            "JOB_BASE":                JOB_BASE,
            "PROJECT_MAP":             JsonOutput.toJson(PROJECT_MAP),
            "DEPENDENCY_PROJECT_LIST": JsonOutput.toJson(DEPENDENCY_PROJECT_LIST),
            "RELEASE":                 it,
            "NET_RETRY_COUNT":         NET_RETRY_COUNT,
            "RETRY_COUNT":             RETRY_COUNT,
            "LOCI_BUILD_SLAVE_LABEL":  LOCI_BUILD_SLAVE_LABEL,
        )
        properties {
            disableResume()
        }
        parameters {
            booleanParam {
                defaultValue(false)
                description('RUN AIO deployment against image(s)')
                name('RUN_DEPLOYMENT')
            }
        }
        definition {
            cps {
                script(readFileFromWorkspace(
                           "${JOB_BASE}/JenkinsfileReleaseNightly")
                )
                sandbox(false)
            }
        }
        triggers {
            cron('H 5 * * *')
        }
    }
}


RELEASES.each {
    pipelineJob("${JOB_BASE}/UpdateMirrors") {
        environmentVariables(
            "JOB_BASE":                JOB_BASE,
            "MIRRORS_PREFIX":          MIRRORS_PREFIX,
            "NET_RETRY_COUNT":         NET_RETRY_COUNT,
        )
        properties {
            disableResume()
        }
        parameters {
            choiceParam {
                choices(RELEASES.join("\n"))
                description("Supported releases: ${RELEASES.join(', ')}")
                name('RELEASE')
            }
        }
        definition {
            cps {
                script(readFileFromWorkspace(
                           "${JOB_BASE}/JenkinsfileUpdateMirrors")
                )
                sandbox(false)
            }
        }
    }
}

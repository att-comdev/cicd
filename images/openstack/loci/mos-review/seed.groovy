import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic

json = new JsonSlurperClassic()

JOB_BASE='images/openstack/loci/mos-review'
folder("${JOB_BASE}")
LOCI_BUILD_SLAVE_LABEL = 'loci_generic_review'
RETRY_COUNT = 2
NET_RETRY_COUNT = 5
RELEASE_BRANCH_MAP = json.parseText(RELEASE_BRANCH_MAP)
SUPPORTED_RELEASES = RELEASE_BRANCH_MAP.keySet() as List
EVENT_TYPES = ['manual', 'patchset-created', 'change-merged']
UPLIFT_COMMIT_MESSAGE_TEMPLATE = "[Uplift] %s %s loci update"
UPLIFT_TOPIC_TEMPLATE = "%s-loci-update"

MIRRORS_PREFIX = 'mirrors/mos/'
REQ_PROJECT_NAME = 'mos-requirements'
PROJECT_MAP = [
    "ocata": [
        "${REQ_PROJECT_NAME}": [],
        'mos-keystone':        [],
        'mos-heat':            [],
        'mos-glance':          [],
        'mos-cinder':          [],
        'mos-horizon':         [],
        'mos-neutron':         ['neutron', 'neutron-sriov'],
        'mos-nova':            ['nova', 'nova-1804'],
    ],
    "stein": [
        "${REQ_PROJECT_NAME}": [],
        'mos-keystone':        [],
        'mos-heat':            [],
        'mos-glance':          [],
        'mos-cinder':          [],
        'mos-horizon':         [],
        'mos-neutron':         ['neutron', 'neutron-sriov'],
        'mos-nova':            ['nova', 'nova-1804'],
        'mos-barbican':        [],
    ]
]

MERGED_MAP = [:]
PROJECT_MAP.each { _, projectMap ->
    projectMap.each { projectName, buildTypes ->
        if (!buildTypes) {
            buildTypes = [projectName.split('-')[-1]]
        }
        if (MERGED_MAP.containsKey(projectName)) {
            mergedBuildTypes = MERGED_MAP[projectName].plus(buildTypes)
        } else {
            mergedBuildTypes = buildTypes as Set
        }
        MERGED_MAP[projectName] = mergedBuildTypes
    }
}

DEPENDENCY_PROJECT_LIST = [
    "ocata": [
        'mos-keystoneclient',
        'mos-neutronclient',
        'mos-novaclient',
        'mos-glance-store',
        'mos-glanceclient',
        'openstack/tap-as-a-service',
        'openstack/tap-as-a-service-dashboard',
    ],
    "stein": [
        'mos-keystoneclient',
        'mos-neutronclient',
        'mos-novaclient',
        'mos-glanceclient',
        'mos-glance-store',
        'mos-neutron-lib',
        'openstack/tap-as-a-service',
        'openstack/tap-as-a-service-dashboard',
    ]
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
        choiceParam (
            'RELEASE',
            SUPPORTED_RELEASES,
            "Supported releases: ${SUPPORTED_RELEASES.join(', ')}"
        )
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
        choiceParam (
            'EVENT_TYPE',
            EVENT_TYPES,
            ('Type of event that triggered job.\n\n'    +
             'Only "manual" is supported for manually ' +
             'triggered jobs'),
        )
        booleanParam {
            defaultValue(true)
            description('RUN AIO deployment against image(s)')
            name('RUN_DEPLOYMENT')
        }
        MERGED_MAP.keySet().each {
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
        "JOB_BASE":                       JOB_BASE,
        "PROJECT_MAP":                    JsonOutput.toJson(PROJECT_MAP),
        "REQ_PROJECT_NAME":               REQ_PROJECT_NAME,
        "RETRY_COUNT":                    RETRY_COUNT,
        "UPLIFT_IMAGES":                  false,
        "UPLIFT_COMMIT_MESSAGE_TEMPLATE": UPLIFT_COMMIT_MESSAGE_TEMPLATE,
        "UPLIFT_TOPIC_TEMPLATE":          UPLIFT_TOPIC_TEMPLATE,
    )
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileGeneric"))
            sandbox(false)
        }
    }
}

MERGED_MAP.each { projectName, buildTypes ->
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
                choiceParam (
                    'RELEASE',
                    SUPPORTED_RELEASES,
                    "Supported releases: ${SUPPORTED_RELEASES.join(', ')}"
                )
                choiceParam (
                    'EVENT_TYPE',
                    EVENT_TYPES,
                    ('Type of event that triggered job.\n\n'    +
                     'Only "manual" is supported for manually ' +
                     'triggered jobs'),
                )
                if (buildType =~ /nova|neutron/) {
                    booleanParam {
                        defaultValue(true)
                        description('Add custom debian repository, specified in ' +
                                    'OVS_REPO config parameter, to base image')
                        name("CUSTOM_OVS")
                    }
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
                "RESTRICT_EVENT_TYPE": true,
                "UPDATE_TOPIC":        UPDATE_TOPIC,
                "PROJECT_NAME":        projectName,
                "BUILD_TYPE":          buildType,
                "NET_RETRY_COUNT":     NET_RETRY_COUNT,
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
        "NET_RETRY_COUNT":    NET_RETRY_COUNT,
        "SUPPORTED_RELEASES": JsonOutput.toJson(SUPPORTED_RELEASES),
        "TROUBLESHOOTING":    false,
        "LABEL":              "",
    )
    parameters {
        stringParam {
            defaultValue('{}')
            description('')
            name('OVERRIDE_IMAGES')
            trim(true)
        }
        choiceParam (
            'RELEASE',
            SUPPORTED_RELEASES,
            "Supported releases: ${SUPPORTED_RELEASES.join(', ')}"
        )
        booleanParam {
            defaultValue(false)
            description('If true deploy from scratch, ' +
                        'if false use precreated k8s snapshot.')
            name('INITIAL_DEPLOYMENT')
        }
        booleanParam {
            defaultValue(false)
            description('Relevant only if INITIAL_DEPLOYMENT is true.\n' +
                        'If true split deployment in two parts - k8s + ceph ' +
                        'and Openstack installations, create temporary ' +
                        'snapshot at the end of first part, use it for ' +
                        'second part and promote in case of success.')
            name('CREATE_SNAPSHOT')
        }
    }
}


pipelineJob("${JOB_BASE}/DebugDeploymentPipeline") {
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
        "NET_RETRY_COUNT":    NET_RETRY_COUNT,
        "SUPPORTED_RELEASES": JsonOutput.toJson(SUPPORTED_RELEASES),
        "CREATE_SNAPSHOT":    false,
        "INITIAL_DEPLOYMENT": false,
        "TROUBLESHOOTING":    true,
        "OVERRIDE_IMAGES":    '{}',
    )
    parameters {
        choiceParam (
            'RELEASE',
            SUPPORTED_RELEASES,
            "Supported releases: ${SUPPORTED_RELEASES.join(', ')}"
        )
        stringParam {
            defaultValue('')
            description('')
            name('LABEL')
            trim(true)
        }
    }
}


pipelineJob("${JOB_BASE}/DebugPipeline") {
    environmentVariables(
        "JOB_BASE":     JOB_BASE,
        "PROJECT_LIST": JsonOutput.toJson(MERGED_MAP.keySet()),
        "RETRY_COUNT":  RETRY_COUNT,
    )
    properties {
        disableResume()
    }
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileDebug"))
            sandbox(false)
        }
    }
    triggers {
        gerritTrigger {
            serverName('mtn5-gerrit')
            gerritProjects {
                SUPPORTED_RELEASES.each { release ->
                    gerritProject {
                        compareType('REG_EXP')
                        pattern(PROJECT_MAP[release].keySet().join("|"))
                        branches {
                            branch {
                                compareType("REG_EXP")
                                pattern("${RELEASE_BRANCH_MAP[release]}")
                            }
                        }
                        topics {
                            topic {
                                compareType('REG_EXP')
                                pattern("^debug.*\$")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                }
            }
            triggerOnEvents {
                patchsetCreated {
                    excludeDrafts(true)
                    excludeTrivialRebase(true)
                    excludeNoCodeChange(true)
                }
                commentAddedContains {
                    commentAddedCommentContains('^[Dd]ebug.*')
                }
            }
            silentMode(true)
        }
    }
}


pipelineJob("${JOB_BASE}/CodeReviewPipeline") {
    parameters {
        stringParam {
            defaultValue('')
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
            defaultValue('master')
            description('')
            name('GERRIT_BRANCH')
            trim(true)
        }
        choiceParam (
            'GERRIT_EVENT_TYPE',
            EVENT_TYPES,
            ('Type of event that triggered job.\n\n'    +
             'Only "manual" is supported for manually ' +
             'triggered jobs'),
        )
    }
    environmentVariables(
        "JOB_BASE":     JOB_BASE,
        "PROJECT_LIST": JsonOutput.toJson(MERGED_MAP.keySet()),
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
                SUPPORTED_RELEASES.each { release ->
                    gerritProject {
                        compareType('REG_EXP')
                        pattern((PROJECT_MAP[release].keySet() +
                                 DEPENDENCY_PROJECT_LIST[release]).join('|'))
                        branches {
                            branch {
                                compareType("REG_EXP")
                                pattern("${RELEASE_BRANCH_MAP[release]}")
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


SUPPORTED_RELEASES.each { release ->
    pipelineJob("${JOB_BASE}/ReleaseNightlyPipeline${release.capitalize()}") {
        environmentVariables(
            "JOB_BASE":                       JOB_BASE,
            "PROJECT_MAP":                    JsonOutput.toJson(PROJECT_MAP[release]),
            "DEPENDENCY_PROJECT_LIST":        JsonOutput.toJson(DEPENDENCY_PROJECT_LIST[release]),
            "RELEASE":                        "${release}",
            "NET_RETRY_COUNT":                NET_RETRY_COUNT,
            "RETRY_COUNT":                    RETRY_COUNT,
            "REQ_PROJECT_NAME":               REQ_PROJECT_NAME,
            "RECREATE_SNAPSHOT":              true,
            "UPLIFT_COMMIT_MESSAGE_TEMPLATE": UPLIFT_COMMIT_MESSAGE_TEMPLATE,
            "UPLIFT_TOPIC_TEMPLATE":          UPLIFT_TOPIC_TEMPLATE,
        )
        properties {
            disableResume()
        }
        parameters {
            booleanParam {
                defaultValue(true)
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


SUPPORTED_RELEASES.each { release ->
    pipelineJob("${JOB_BASE}/LatestUpliftPipeline${release.capitalize()}") {
        environmentVariables(
            "JOB_BASE":                       JOB_BASE,
            "PROJECT_MAP":                    JsonOutput.toJson(PROJECT_MAP[release]),
            "DEPENDENCY_PROJECT_LIST":        JsonOutput.toJson(DEPENDENCY_PROJECT_LIST[release]),
            "RELEASE":                        "${release}",
            "NET_RETRY_COUNT":                NET_RETRY_COUNT,
            "RETRY_COUNT":                    RETRY_COUNT,
            "REQ_PROJECT_NAME":               REQ_PROJECT_NAME,
            "RECREATE_SNAPSHOT":              false,
            "RUN_DEPLOYMENT":                 true,
            "UPLIFT_COMMIT_MESSAGE_TEMPLATE": UPLIFT_COMMIT_MESSAGE_TEMPLATE,
            "UPLIFT_TOPIC_TEMPLATE":          UPLIFT_TOPIC_TEMPLATE,
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
}


pipelineJob("${JOB_BASE}/UpdateMirrors") {
    environmentVariables(
        "JOB_BASE":                JOB_BASE,
        "MIRRORS_PREFIX":          MIRRORS_PREFIX,
        "NET_RETRY_COUNT":         NET_RETRY_COUNT,
        "REQ_PROJECT_NAME":        REQ_PROJECT_NAME,
    )
    properties {
        disableResume()
    }
    parameters {
        choiceParam (
            'RELEASE',
            SUPPORTED_RELEASES,
            "Supported releases: ${SUPPORTED_RELEASES.join(', ')}"
        )
    }
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileUpdateMirrors"))
            sandbox(false)
        }
    }
}


pipelineJob("${JOB_BASE}/UpliftPipeline") {
    environmentVariables(
        "NET_RETRY_COUNT":    NET_RETRY_COUNT,
        "SUPPORTED_RELEASES": JsonOutput.toJson(SUPPORTED_RELEASES),
    )
    properties {
        disableResume()
    }
    parameters {
        choiceParam (
            'RELEASE',
            SUPPORTED_RELEASES,
            "Supported releases: ${SUPPORTED_RELEASES.join(', ')}"
        )
        stringParam {
            description("Images map to uplift")
            defaultValue('{}')
            name('IMAGES')
            trim(true)
        }
        stringParam {
            description("Gerrit topic for uplift change")
            defaultValue('{}')
            name('TOPIC')
            trim(true)
        }
        stringParam {
            description("Commit message for uplift commit")
            defaultValue('[DO NOT MERGE] For test only')
            name('COMMIT_MESSAGE')
            trim(true)
        }
    }
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_BASE}/JenkinsfileUplift"))
            sandbox(false)
        }
    }
}

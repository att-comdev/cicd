import com.att.nccicd.config.conf as config
import groovy.json.JsonSlurperClassic

conf = new config(env).CONF

json = new JsonSlurperClassic()
projectList = json.parseText(PROJECT_LIST)
RELEASE_BRANCH_MAP = json.parseText(RELEASE_BRANCH_MAP)

def keyForValue(map, value) {
    map.find { it.value == value }?.key
}

RELEASE = keyForValue(RELEASE_BRANCH_MAP, GERRIT_BRANCH)

if ( params.GERRIT_TOPIC =~ /^debug.*/ ) {
    error ("Pipeline is disabled for changes with debug topic")
}

ut_params = [
    stringParam(name: 'PROJECT_NAME', value: GERRIT_PROJECT),
    stringParam(name: 'PROJECT_REF', value: GERRIT_REFSPEC),
    stringParam(name: 'PROJECT_BRANCH', value: GERRIT_BRANCH),
]

runningSet = [:]

runningSet['codeReview'] = {
    stage("Code-review") {
        if (GERRIT_EVENT_TYPE != 'change-merged' &&
                !conf.UT_SKIP_LIST.contains(GERRIT_PROJECT)) {
            print "Building review job with ${ut_params}"
            job = utils.runBuild("${JOB_BASE}/code-review", ut_params)
        } else {
            print "Skipping checks"
        }
    }
}

def getRefParamName(String project) {
    project.toUpperCase().replace('-', '_') + "_REF"
}

def compileDependencies(Map dependencyMap) {
    res = []
    dependencyMap.each { name, ref -> res.add("${name}:${ref}") }
    res.join(" ")
}

if (projectList.contains(GERRIT_PROJECT)) {
    parameters = [
        stringParam(name: getRefParamName(GERRIT_PROJECT),
                    value: GERRIT_REFSPEC),
    ]
} else {
    parameters = [
        stringParam(
            name: 'DEPENDENCY_LIST',
            value: compileDependencies([(GERRIT_PROJECT): GERRIT_REFSPEC])
        ),
    ]
}
parameters.add(stringParam(name: 'EVENT_TYPE', value: GERRIT_EVENT_TYPE))
parameters.add(stringParam(name: 'RELEASE', value: RELEASE))

runningSet['genericPipeline'] = {
    print "Building Generic Pipeline with ${parameters}"
    stage("Generic Pipeline") {
        job = utils.runBuild("${JOB_BASE}/GenericPipeline", parameters)
        currentBuild.description = job.getBuildVariables()["IMAGES"]
    }
}

parallel runningSet

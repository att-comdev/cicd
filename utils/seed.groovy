import groovy.json.JsonSlurper

//This will change once all images/airship/xxx are deprecated
def imagesJson = '''{ "github":[{
                                "repo":"https://git.openstack.org/openstack/airship-pegleg",
                                "directory":"utils",
                                "image":"pegleg",
                                "name":"pegleg"
                            }]}'''

jsonSlurper = new JsonSlurper()
object = jsonSlurper.parseText(imagesJson)

for (entry in object.github) {
    folder("${entry.directory}")
    pipelineJob("${entry.directory}/${entry.image} - Regenerate Certificate") {
        logRotator {
            daysToKeep(90)
        }
        parameters {
            stringParam {
                defaultValue("DESTINATION_DIRECTORY")
                description('Destination Directory')
                name('DESTINATION_DIRECTORY')
            }
            stringParam {
                defaultValue("SITE")
                description('Name of site')
                name('PROJECT_SITE')
            }
        }
        configure {
            node ->
                node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl' {
                    durationName 'hour'
                    count '10'
                }
        }

        definition {
            cps {
                script(readFileFromWorkspace("utils/Jenkinsfile"))
                sandbox(false)
            }
        }
    }
}
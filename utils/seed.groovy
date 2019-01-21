import groovy.json.JsonSlurper

//This will change once all images/airship/xxx are deprecated
def imagesJson = '''{ "github":[{
                                "repo":"https://git.openstack.org/openstack/airship-pegleg",
                                "directory":"utils",
                                "image":"pegleg",
                                "name":"pegleg"
                            }]}'''

def sites = ['mtn57a', 'auk3', 'mtn13b.1', 'mtn13b.2', 'mtn13b.3', 'mtn52a', 'mtn52e', 'mtn29', 'mdt6']

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
            choiceParam ('PROJECT_SITE', "${sites}", 'Name of site')
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
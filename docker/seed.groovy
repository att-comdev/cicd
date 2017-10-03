import groovy.json.JsonSlurper

def chartsJson = '''{ "images":[{
                        "repo":"att-comdev/dockerfiles",
                        "charts":[  "airflow",
                                    "maas",
                                    "rabbitmq"]                             
                        }]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

for (entry in object.images) {
    for (chart in entry.charts) {
        pipelineJob("dockerimages/${entry.repo}/${chart}") {

            triggers {
                gerritTrigger {
                    serverName('Gerrithub-jenkins')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("${entry.repo}")
                            branches {
                                branch {
                                compareType("ANT")
                                pattern("**")
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
                } 

                }

                definition {
                    cps {
                        script(readFileFromWorkspace('docker/Jenkinsfile'))
                        sandbox()
                    }
                }
            }
        }
    }
}


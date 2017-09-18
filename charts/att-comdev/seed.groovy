import groovy.json.JsonSlurper

def chartsJson = '''{ "aic":[{
                        "repo":"att-comdev/aic-helm",
                        "charts":[  "airflow",
                                    "deckhand",
                                    "drydock",
                                    "shipyard"]}]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)
for (entry in object.aic) {
  for (chart in entry.charts) {
    pipelineJob("charts/${entry.repo}/${chart}") {
      
      triggers {
        // http://<gerrit>/plugin/job-dsl/api-viewer/index.html#path/javaposse.jobdsl.dsl.helpers.properties.PropertiesContext.pipelineTriggers-triggers-gerrit
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
              filePaths {
                filePath {
                  compareType("ANT")
                  pattern("$chart/**")
                }
                filePath {
                  compareType("ANT")
                  pattern("helm-toolkit/**")
                }
              }
              disableStrictForbiddenFileVerification(false)
            }
          }
          triggerOnEvents {
            changeMerged()
          }
        }
        
        definition {
          cps {
            script(readFileFromWorkspace('aic-helm/Jenkinsfile'))
            sandbox()
          }
        }
      }
    }
  }
}

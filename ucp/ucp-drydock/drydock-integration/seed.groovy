
import groovy.json.JsonSlurper
def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

JOB_PATH

        pipelineJob('ucp/ucp-drydock/drydock-integration') {
            triggers {
            // http://<gerrit>/plugin/job-dsl/api-viewer/index.html#path/javaposse.jobdsl.dsl.helpers.properties.PropertiesContext.pipelineTriggers-triggers-gerrit
                gerritTrigger {
                    serverName('Gerrithub-jenkins')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("att-comdev/cicd")
                            branches {
                                branch {
                                compareType("ANT")
                                pattern("**")
                                }
                            }
                            filePaths {
                                filePath {
                                compareType("ANT")
                                pattern("ucp/ucp-drydock/drydock-integration/**")
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
                    }
                }

                definition {
                    cps {
                        script(readFileFromWorkspace('ucp/ucp-drydock/drydock-integration/Jenkinsfile'))
                        sandbox()
                    }
                }
            }
        }

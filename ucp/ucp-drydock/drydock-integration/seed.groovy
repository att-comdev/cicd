//import groovy.json.JsonSlurper
//def jsonSlurper = new JsonSlurper()

base_path="ucp/ucp-drydock/drydock-integration"
pipelineJob("${base_path}") {
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
                                  pattern("${base_path}/**")
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
                      script(readFileFromWorkspace('${base_path}/Jenkinsfile'))
                        sandbox()
                    }
                }
            }
        }


 import groovy.json.JsonSlurper 
 
 
 def chartsJson = '''{ "kolla":[{ 
                         "repo":"kolla_build/newton", 
                         "charts":[  "cinder", 
                                     "heat", 
                                     "glance", 
                                     "horizon", 
                                     "keystone", 
                                     "neutron", 
                                     "nova", 
                                     "barbican"] 
                         }]}''' 
 
 
 def jsonSlurper = new JsonSlurper() 
 def object = jsonSlurper.parseText(chartsJson) 
 
 
 for (entry in object.kolla) { 
     for (chart in entry.charts) { 
         pipelineJob("kolla/${entry.repo}/${chart}") { 
 
 
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
                             } 
                             disableStrictForbiddenFileVerification(false) 
                         } 
                     } 
                     triggerOnEvents { 
                        patchsetCreated{
                           excludeDrafts(false)
                           excludeTrivialtRebase(false)
                           excludeNoCodeChange(false)
                          }
                         changeMerged() 
                     } 
                 } 
 
 
                 definition { 
                     cps { 
                         script(readFileFromWorkspace('kolla-newton/Jenkinsfile)) 
                         sandbox() 
                     } 
                 } 
             } 
         } 
     } 
 } 


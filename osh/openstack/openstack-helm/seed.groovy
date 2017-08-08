
def charts = ['cinder', 'glance', 'heat', 'horizon', 'keystone', 'magnum', 'mistral', 'neutron', 'nova']

for (chart in charts) {
   pipelineJob("osh/openstack/openstack-helm/${chart}") {

      wrappers {
         jCloudsOneOffSlave()
      }

      triggers {
         // http://<gerrit>/plugin/job-dsl/api-viewer/index.html#path/javaposse.jobdsl.dsl.helpers.properties.PropertiesContext.pipelineTriggers-triggers-gerrit
         gerritTrigger {
            serverName('OS-CommunityGerrit')
            gerritProjects {
               gerritProject {
                  compareType('PLAIN')
                  pattern('openstack/openstack-helm')
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
               script(readFileFromWorkspace('osh/openstack/openstack-helm/Jenkinsfile'))
               sandbox()
            }
         }

      }
   }
}


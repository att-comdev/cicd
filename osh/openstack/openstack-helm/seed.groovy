def comps = ['glance', 'nova', 'cinder', 'neutron']
def gtdata = 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data'


for (comp in comps) {
   pipelineJob("osh/openstack/openstack-helm/${comp}") {

      // http://<gerrit>/plugin/job-dsl/api-viewer/index.html#path/javaposse.jobdsl.dsl.helpers.properties.PropertiesContext.pipelineTriggers-triggers-gerrit
      triggers {
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
                        pattern("$comp/**")
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


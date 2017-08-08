def comps = ['glance', 'nova', 'cinder', 'neutron']
def gtdata = 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data'


for (comp in comps) {
  pipelineJob("osh/openstack/openstack-helm/${comp}") {

    triggers {
        gerrit {
          events {
            patchsetCreated()
            changeMerged()
          }
          project('plain:openstack/openstack-helm', ['ant:**'])

          // file path not supported via Jenkins DSL
          configure {
            it / gerritProjects / "${gtdata}.GerritProject" / filePaths << "${gtdata}.FilePath" {
                compareType 'ANT'
                pattern "$comp/**"
              
            }
            it / gerritProjects / "${gtdata}.GerritProject" / filePaths << "${gtdata}.FilePath" {
                compareType 'ANT'
                pattern 'helm-toolkit/**'
            }
          }
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


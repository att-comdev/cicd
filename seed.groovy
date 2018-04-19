import groovy.json.JsonSlurper

/**
 * Create Jenkins Dashboard
 * This section will create all the Views and folders needed for
 * pipelines
 *
 *
**/
def createDashboardJson = ''' {"viewList":[{
                                            "viewName":"Charts",
                                            "jobList":[  "charts" ]
                                         },{
                                            "viewName":"Images",
                                            "jobList":[ "images" ]
                                         },{
                                            "viewName":"Integration",
                                            "jobList":[ "integration" ]
                                         },{
                                            "viewName":"Packages",
                                            "jobList":[ "packages" ]
                                         },{
                                            "viewName":"Test",
                                            "jobList":[ "test" ]
                                         }],
                               "folderList":[
                                           "charts",
                                            "charts/att-comdev",
                                            "openstack",
                                            "openstack/openstack-helm",
                                            "openstack/openstack-helm-addons",
                                            "openstack/openstack-helm-infra",
                                            "images",
                                            "images/att-comdev",
                                            "images/att-comdev/armada",
                                            "images/att-comdev/deckhand",
                                            "images/att-comdev/promenade",
                                            "images/att-comdev/drydock",
                                            "images/att-comdev/berth",
                                            "images/att-comdev/divingbell",
                                            "images/att-comdev/shipyard",
                                            "images/att-comdev/shipyard/airflow",
                                            "images/att-comdev/nagios",
                                            "images/att-comdev/maas",
                                            "images/att-comdev/maas/maas-rack-controller",
                                            "images/att-comdev/maas/maas-region-controller",
                                            "images/att-comdev/pegleg",
                                            "images/calico",
                                            "images/kubernetes",
                                            "images/openstack",
                                            "images/openstack/ceph-config-helper",
                                            "images/openstack/gate-utils",
                                            "images/openstack/kolla",
                                            "images/openstack/libvirt",
                                            "images/openstack/loci",
                                            "images/ranger-agent",
                                            "images/vbmc",
                                            "images/kolla",
                                            "images/kolla/community",
                                            "images/kolla/mos",
                                            "images/loci",
                                            "images/loci/community",
                                            "images/loci/mos",
                                            "integration",
                                            "integration/ucp-deploy",
                                            "integration/aql",
                                            "integration/genesis-integration",
                                            "packages",
                                            "test",
                                            "TestJobs",
                                            "CICD"
                                         ]}
'''


def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(createDashboardJson)

for (aFolder in object.folderList) {
  folder("${aFolder}")
}

for(aView in object.viewList){
      listView("${aView.viewName}") {
            description()
            filterBuildQueue()
            filterExecutors()
            jobs {
                for(aJob in aView.jobList){
                  name("${aJob}")
                }
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }
}

/**
 * End of Create Dashboard seed.groovy 
 **/

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
                                            "charts/openstack",
                                            "charts/openstack/openstack-helm",
                                            "charts/openstack/openstack-helm-addons",
                                            "charts/openstack/openstack-helm-infra",
                                            "charts/openstack/openstack-helm-addons/jenkins",
                                            "charts/openstack/openstack-helm-addons/artifactory",
                                            "charts/openstack/openstack-helm/cinder",
                                            "charts/openstack/openstack-helm-infra/elasticsearch",
                                            "charts/openstack/openstack-helm-infra/fluentd",
                                            "charts/openstack/openstack-helm-infra/kibana",
                                            "charts/openstack/openstack-helm-infra/postgresql",
                                            "charts/openstack/openstack-helm-infra/calico",
                                            "charts/openstack/openstack-helm-infra/flannel",
                                            "charts/openstack/openstack-helm-infra/grafana",
                                            "charts/openstack/openstack-helm-infra/kube-dns",
                                            "charts/openstack/openstack-helm-infra/nfs-provisioner",
                                            "charts/openstack/openstack-helm-infra/prometheus-alertmanager",
                                            "charts/openstack/openstack-helm-infra/prometheus-kube-state-metrics",
                                            "charts/openstack/openstack-helm-infra/prometheus-openstack-exporter",
                                            "charts/openstack/openstack-helm-infra/prometheus",
                                            "charts/openstack/openstack-helm-infra/redis",
                                            "charts/openstack/openstack-helm-infra/registry",
                                            "charts/openstack/openstack-helm-infra/tiller",
                                            "charts/openstack/openstack-helm/heat",
                                            "charts/openstack/openstack-helm/helm-toolkit",
                                            "charts/openstack/openstack-helm/glance",
                                            "charts/openstack/openstack-helm/horizon",
                                            "charts/openstack/openstack-helm/keystone",
                                            "charts/openstack/openstack-helm/magnum",
                                            "charts/openstack/openstack-helm/mistral",
                                            "charts/openstack/openstack-helm/neutron",
                                            "charts/openstack/openstack-helm/nova",
                                            "charts/openstack/openstack-helm/barbican",
                                            "charts/openstack/openstack-helm/ceph",
                                            "charts/openstack/openstack-helm/etcd",
                                            "charts/openstack/openstack-helm/ingress",
                                            "charts/openstack/openstack-helm/mariadb",
                                            "charts/openstack/openstack-helm/libvirt",
                                            "charts/openstack/openstack-helm/openvswitch",
                                            "charts/openstack/openstack-helm/memcached",
                                            "charts/openstack/openstack-helm/postgresql",
                                            "charts/openstack/openstack-helm/rabbitmq",
                                            "charts/openstack/openstack-helm/rally",
                                            "charts/openstack/openstack-helm/senlin",
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
                                            "images/att-comdev/ubuntu",
                                            "images/att-comdev/ubuntu/cloud",
                                            "images/att-comdev/ubuntu/docker",
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

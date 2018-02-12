
import groovy.json.JsonSlurper

def chartsJson = '''{ "osh":[{
                        "repo":"openstack/openstack-helm",
                        "charts":[  "barbican",
                                    "ceilometer",
                                    "cinder",
                                    "heat",
                                    "helm-toolkit",
                                    "glance",
                                    "horizon",
                                    "keystone",
                                    "magnum",
                                    "mistral",
                                    "neutron",
                                    "nova",
                                    "barbican",
                                    "ceph",
                                    "ceph-client",
                                    "ceph-mon",
                                    "ldap",
                                    "ceph-osd",
                                    "etcd",
                                    "ingress",
                                    "mariadb",
                                    "tempest",
                                    "libvirt",
                                    "openvswitch",
                                    "memcached",
                                    "postgresql",
                                    "rabbitmq",
                                    "rally",
                                    "senlin"]
                        },{
                        "repo":"openstack/openstack-helm-addons",
                        "charts":[
                                    "artifactory",
                                    "jenkins",
                                    "ranger-agent"
                                 ]},{
                        "repo":"openstack/openstack-helm-infra",
                        "charts":[  "elasticsearch",
                                    "fluentd",
                                    "flannel",
                                    "fluent-logging",
                                    "helm-toolkit",
                                    "kibana",
                                    "postgresql",
                                    "calico",
                                    "flannel",
                                    "grafana",
                                    "kube-dns",
                                    "ldap",
                                    "nagios",
                                    "kubernetes-keystone-webhook",
                                    "nfs-provisioner",
                                    "prometheus-alertmanager",
                                    "prometheus-node-exporter",
                                    "prometheus-kube-state-metrics",
                                    "prometheus-openstack-exporter",
                                    "prometheus",
                                    "redis",
                                    "registry",
                                    "tiller"]}

]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

for (entry in object.osh) {
    for (chart in entry.charts) {
        pipelineJob("charts/${entry.repo}/${chart}") {
           // disabled(SILENT_MODE.toBoolean())
            disabled(false)
            parameters {
                stringParam('REPO',"${entry.repo}")
                stringParam('RELEASE_CURRENT_KEY',"RDM9.chart.${chart}.dev.current")
                stringParam('RELEASE_STATUS_KEY',"RDM9.chart.${chart}.dev.status")
            }
            triggers {
            // http://<gerrit>/plugin/job-dsl/api-viewer/index.html#path/javaposse.jobdsl.dsl.helpers.properties.PropertiesContext.pipelineTriggers-triggers-gerrit
                gerritTrigger {
                    silentMode(SILENT_MODE.toBoolean())
                    serverName('OS-CommunityGerrit')
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
                        script(readFileFromWorkspace('charts/openstack/Jenkinsfile'))
                        sandbox()
                    }
                }
            }
        }
    }
}
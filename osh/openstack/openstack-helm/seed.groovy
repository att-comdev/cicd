
import groovy.json.JsonSlurper

def chartsJson = '''{ "osh":[{
                        "repo":"openstack/openstack-helm",
                        "charts":[  "cinder",
                                    "heat",
                                    "glance",
                                    "horizon",
                                    "keystone",
                                    "magnum",
                                    "mistral",
                                    "neutron",
                                    "nova",
                                    "barbican",
                                    "ceph",
                                    "dns-helper",
                                    "etcd",
                                    "ingress",
                                    "mariadb",
                                    "memcached",
                                    "rabbitmq",
                                    "senlin"]
                        },{
                        "repo":"openstack/openstack-helm-addons",
                        "charts":[  "elasticsearch",
                                    "fluentd",
                                    "kibana",
                                    "postgresql"]}]}'''

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(chartsJson)

for (entry in object.osh) {
    for (chart in entry.charts) {
        pipelineJob("osh/${entry.repo}/${chart}") {

            triggers {
            // http://<gerrit>/plugin/job-dsl/api-viewer/index.html#path/javaposse.jobdsl.dsl.helpers.properties.PropertiesContext.pipelineTriggers-triggers-gerrit
                gerritTrigger {
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
                        script(readFileFromWorkspace('osh/openstack/openstack-helm/Jenkinsfile'))
                        sandbox()
                    }
                }
            }
        }
    }
}
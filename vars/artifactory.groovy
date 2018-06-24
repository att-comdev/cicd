import att.comdev.cicd.config.conf

import groovy.json.JsonOutput

private def spec(pattern, target) {
    spec = ["files": [["pattern": pattern,
                       "target": target,
                       "flat": true]]]
    return new JsonOutput().toJson(spec)
}

def publish(pattern, target) {
    info = Artifactory.server(conf.ARTF_SERVER_ID).upload(spec(pattern, target))
    Artifactory.server(conf.ARTF_SERVER_ID).publishBuildInfo(info)
}

def download(pattern, target) {
    Artifactory.server(conf.ARTF_SERVER_ID).download(spec(pattern, target))
}
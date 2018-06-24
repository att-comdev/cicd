import att.comdev.cicd.config.conf

import groovy.json.JsonOutput

private def spec(file, target) {
    spec = ["files": [["pattern": file,
                       "target": target,
                       "flat": true]]]
    return new JsonOutput().toJson(spec)
}

def publish(file, target) {
    info = Artifactory.server(conf.ARTF_SERVER_ID).upload(spec(file, target))
    Artifactory.server(conf.ARTF_SERVER_ID).publishBuildInfo(info)
}

def download(file, target) {
    Artifactory.server(conf.ARTF_SERVER_ID).download(spec(file, target))
}
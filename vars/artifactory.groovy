import att.comdev.cicd.config.conf

import groovy.json.JsonOutput

def artf = Artifactory.server(conf.ARTF_SERVER_ID)

private def spec(pattern, target) {
    spec = ["files": [["pattern": pattern,
                       "target": target,
                       "flat": true]]]
    return new JsonOutput().toJson(spec)
}

def publish(pattern, target) {
    info = artf.upload(spec(pattern, target))
    artf.publishBuildInfo(info)
}

def download(pattern, target) {
    artf.download(spec(pattern, target))
}
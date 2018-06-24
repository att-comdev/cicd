import att.comdev.cicd.config.conf

import groovy.json.JsonOutput

def artf = Artifactory.server("artifactory")

private def spec(pattern, target) {
    spec = ["files": [["pattern": pattern,
                       "target": target,
                       "flat": true]]]
    return new JsonOutput().toJson(spec)
}

def publish(pattern, target) {
    info = artf.server("artifactory").upload(spec(pattern, target))
    artf.server("artifactory").publishBuildInfo(info)
}

def download(pattern, target) {
    Artifactory.server("artifactory").download(spec(pattern, target))
}
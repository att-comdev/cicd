import att.comdev.cicd.config.conf

import groovy.json.JsonOutput

/**
 * Helper method to JSON-ify the request we send off to Artifactory
 * for file upload/download
 *
 * @param file The file we're interested in uploading/downloading
 * @param target Where, in Artifactory, we want to grab/place this file
 * @return spec Translation into JSON of the file to upload/download and its path
 */
private def spec(file, target) {
    spec = ["files": [["pattern": file,
                       "target": target,
                       "flat": true]]]
    return new JsonOutput().toJson(spec)
}

/**
 * Perform the upload to Artifactory, given the file and
 * target/path.
 *
 * @param file The file we're interested in uploading
 * @param target Where, in Artifactory, we want to place this file
 */
def upload(file, target) {
    info = Artifactory.server(conf.ARTF_SERVER_ID).upload(spec(file, target))
    Artifactory.server(conf.ARTF_SERVER_ID).publishBuildInfo(info)
}

/**
 * Perform the download from Artifactory, given the file and
 * target/path.
 *
 * @param file The file we're interested in downloading
 * @param target Where, in Artifactory, we want to grab this file from
 */
def download(file, target) {
    Artifactory.server(conf.ARTF_SERVER_ID).download(spec(file, target))
}
/**
 * Setup docker within docker (to get latest docker version on Ubuntu 16.04)
 * requires setting {"storage-driver": "overlay2"} option in docker.json
 *
 * @param dockerImage The docker dind image
 * @param artifactoryURL The docker repository URL
 * @param artifactoryCred Credentialsid for authenticating the docker repository
 * @param containerName Name of the running Dind Container
 */
def setupDocker(String dockerImage, String artifactoryURL, String artifactoryCred, String containerName) {
    def opts = '--privileged --name ${containerName} -e HTTP_PROXY="${HTTP_PROXY}" -e HTTPS_PROXY="${HTTPS_PROXY}" '
    def mounts = '-v /var/lib/docker' +
        ' -v $(pwd):/opt/loci'

    // cmd for running Docker in Docker
    dind = "sudo docker exec ${containerName}"

    sh "sudo docker run -d ${opts} ${mounts} ${dockerImage}"
    sh "${dind} sh -cx 'apk update; apk add git'"

    withCredentials([usernamePassword(credentialsId: artifactoryCred,
            usernameVariable: 'ARTIFACTORY_USER',
            passwordVariable: 'ARTIFACTORY_PASSWORD')]) {

        opts = '-u $ARTIFACTORY_USER -p $ARTIFACTORY_PASSWORD'
        sh "${dind} docker login ${opts} ${artifactoryURL}"
    }
}

/**
 * Setup local web server
 * enables localized use of git/docker artifacts
 * exposes mounted volume /opt/loci/web
 * requires setting {"storage-driver": "overlay2"} option in docker.json
 *
 * @param containerName Name of the running Dind Container
 * @param nginxImage The Nginx image
 * @param port Local port for the ngnix service
 */
def setupNginx(String containerName, String nginxImage, String localPort) {
    sh "mkdir -p web"

    // cmd for running Docker in Docker
    dind = "sudo docker exec ${containerName}"

    def opts = '-d -v /opt/loci/web:/usr/share/nginx/html:ro'
    def port = "-p ${localPort}:80"
    sh "${dind} docker run ${opts} ${port} ${nginxImage}"
}

// todo - move to global library if useful
/**
 * Retrieve latest commitid for a specific branch
 * Used for labelling images of Manual triggers when GERRIT_CHANGE_ID is empty
 *
 * @param url Git url
 * @param branch branchname
 * @param targetDirectory local directory where to clone repo
 */
def getVersion(String url, String branch, String targetDirectory) {
    gerrit.cloneProject(url, branch, "", targetDirectory)
    dir (targetDirectory) {
        def cmd = "git rev-parse HEAD"
        return sh(returnStdout: true, script: cmd).trim()
    }
}

/**
 * Export the filesystem structure of a running container
 * Used for building loci images with Wheels export
 *
  * @param containerName Name of the running Dind Container
 * @param requirementsImage Requirements image used to create the wheels export
 */
def exportWheels(String containerName, String requirementsImage) {
    sh "mkdir -p web/images"

    // cmd for running Docker in Docker
    dind = "sudo docker exec ${containerName}"

    sh "${dind} docker create --name loci-wheels ${requirementsImage} bash"
    sh "${dind} docker export -o /opt/loci/web/images/wheels.tar loci-wheels"
    sh "${dind} chmod +r /opt/loci/web/images/wheels.tar"
}

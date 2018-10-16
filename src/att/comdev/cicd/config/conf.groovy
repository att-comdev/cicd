package att.comdev.cicd.config

class conf {

    // Static Images
    public static final String PEGLEG_IMAGE = "artifacts-aic.atlantafoundry.com/att-comdev/pegleg:ef47933903047339bd63fcfa265dfe4296e8a322"
    public static final String PROMENADE_IMAGE = "artifacts-aic.atlantafoundry.com/att-comdev/promenade@sha256:e8a6d6e671fa330e63a91b978757c3cde241aad81f2166aebc9a0880702c0f7c"

    // Artifactory
    public static final String ARTF_SERVER_ID = "artifactory"
    public static final String ARTF_WEB_URL = "https://artifacts-aic.atlantafoundry.com/artifactory"

    public static final String ARTF_LOGS_BASE = "logs/${JOB_NAME}/${BUILD_ID}"
    public static final String ARTF_LOGS_URL = "${ARTF_WEB_URL}/${ARTF_LOGS_BASE}"

    //docker-in-docker image
    public static final String DIND_IMAGE = "docker:17.07.0-dind"

    //nginx image
    public static final String NGINX_IMAGE = "nginx"

    // Other
    public static final String EXCLUDE_NODES = "master jenkins-node-launch 10.24.20.18-slave 10.24.20.19-slave MyCodeReviewDev att-comdev-charts-dev cab24-r820-18 cab24-r820-19 openstack-helm-charts genesis clcp-seaworthy-genesis 5ec-seaworthy-genesis"

}

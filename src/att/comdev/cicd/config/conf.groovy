package att.comdev.cicd.config

class conf {

    // Static Images
    public static final String PEGLEG_IMAGE = "artifacts-aic.atlantafoundry.com/att-comdev/pegleg:ef47933903047339bd63fcfa265dfe4296e8a322"
    public static final String PROMENADE_IMAGE = "artifacts-aic.atlantafoundry.com/att-comdev/promenade@sha256:e8a6d6e671fa330e63a91b978757c3cde241aad81f2166aebc9a0880702c0f7c"

    // Artifactory
    public static final String ARTF_SERVER_ID = "artifactory"
    public static final String ARTF_WEB_URL = "https://artifacts-aic.atlantafoundry.com/artifactory"

    //docker-in-docker image
    public static final String DIND_IMAGE = "docker:17.07.0-dind"

    //nginx image
    public static final String NGINX_IMAGE = "nginx"

    //Helm Repo lists
    public static final Map HELM_REPO = ['stable' : 'https://kubernetes-charts.storage.googleapis.com/',
                                         'incubator' : 'https://kubernetes-charts-incubator.storage.googleapis.com/']

    // Other
    public static final String EXCLUDE_NODES = "master jenkins-node-launch 10.24.20.18-slave 10.24.20.19-slave MyCodeReviewDev att-comdev-charts-dev cab24-r820-18 cab24-r820-19 openstack-helm-charts genesis clcp-seaworthy-genesis 5ec-seaworthy-genesis"

    // GitHub
    public static final String GITHUB_URL = "github.com"
    public static final String JENKINS_GITHUB_CRED_ID = "github-key"
    public static final String GITHUB_REPO_PATH_PREFIX =  ""
    public static final String GITHUB_REPO_PREFIX = ""
}

import org.junit.*
import com.lesfurets.jenkins.unit.*
import org.yaml.snakeyaml.Yaml
import static groovy.test.GroovyAssert.*

class CicdHelperTest extends BasePipelineTest {

    def cicdHelper
    def commonAddressesYaml = [data: [dns: [ingress_domain: "ingress.domain",
                                            service_ip: "127.0.0.1"],
                                      genesis: [hostname: "genesis.hostname"]]]

    @Before
    void setUp() {
        super.setUp()
        // load creds
        cicdHelper = loadScript("vars/cicd_helper.groovy")
    }

    @Test
    void testGetShipyardCreds() {
        def yaml = [data: "shipyard.creds"]

        // create mock readYaml step
        helper.registerAllowedMethod("readYaml", [ LinkedHashMap ]) { yaml }

        // call getShipyardCreds and check result
        def result = cicdHelper.getShipyardCreds("nope")
        assertEquals yaml.data, result
    }

    @Test
    void testGetIPMICreds() {
        def yaml = [data: "ipmi.creds"]

        // create mock readYaml step
        helper.registerAllowedMethod("readYaml", [ LinkedHashMap ]) { yaml }

        // call getIPMICreds and check result
        def result = cicdHelper.getIPMICreds("nope")
        assertEquals yaml.data, result
    }

    @Test
    void testGetDomain() {
        // create mock readYaml step
        helper.registerAllowedMethod("readYaml", [ LinkedHashMap ]) { commonAddressesYaml }

        // call getDomain and check result
        def result = cicdHelper.getDomain("nope")
        assertEquals commonAddressesYaml.data.dns.ingress_domain, result
    }

    @Test
    void testGetGenesisHostname() {
        // create mock readYaml step
        helper.registerAllowedMethod("readYaml", [ LinkedHashMap ]) { commonAddressesYaml }

        // call getGenesisHostname and check result
        def result = cicdHelper.getGenesisHostname("nope")
        assertEquals commonAddressesYaml.data.genesis.hostname, result
    }

    @Test
    void testGetCoreDNS() {
        // create mock readYaml step
        helper.registerAllowedMethod("readYaml", [ LinkedHashMap ]) { commonAddressesYaml }

        // call getCoreDNS and check result
        def result = cicdHelper.getCoreDNS("nope")
        assertEquals commonAddressesYaml.data.dns.service_ip, result
    }

    @Test
    void testPodExecutorConfig() {

        // setup
        Yaml yaml = new Yaml()

        // call podExecutorConfig - all defaults
        def result = cicdHelper.podExecutorConfig()
        def resultYaml = yaml.load(result)

        assertEquals "jenkins/jnlp-slave:alpine", resultYaml.spec.containers[0].image
        assertEquals 0, resultYaml.spec.securityContext.runAsUser
        assertEquals "jenkins-node-primary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key
        assertEquals "jenkins-node-secondary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[1].preference.matchExpressions[0].key

        // call podExecutorConfig - override jnlpImage
        result = cicdHelper.podExecutorConfig("docker.foo.bar/jenkins/jnlp-slave:alpine")
        resultYaml = yaml.load(result)

        assertEquals "docker.foo.bar/jenkins/jnlp-slave:alpine", resultYaml.spec.containers[0].image
        assertEquals 0, resultYaml.spec.securityContext.runAsUser
        assertEquals "jenkins-node-primary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key
        assertEquals "jenkins-node-secondary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[1].preference.matchExpressions[0].key

        // call podExecutorConfig - override runAsUser
        result = cicdHelper.podExecutorConfig("jenkins/jnlp-slave:alpine", "1")
        resultYaml = yaml.load(result)

        assertEquals "jenkins/jnlp-slave:alpine", resultYaml.spec.containers[0].image
        assertEquals 1, resultYaml.spec.securityContext.runAsUser
        assertEquals "jenkins-node-primary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key
        assertEquals "jenkins-node-secondary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[1].preference.matchExpressions[0].key

        // call podExecutorConfig - override priAffinityKey
        result = cicdHelper.podExecutorConfig("jenkins/jnlp-slave:alpine", "0", "foo-bar-primary")
        resultYaml = yaml.load(result)

        assertEquals "jenkins/jnlp-slave:alpine", resultYaml.spec.containers[0].image
        assertEquals 0, resultYaml.spec.securityContext.runAsUser
        assertEquals "foo-bar-primary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key
        assertEquals "jenkins-node-secondary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[1].preference.matchExpressions[0].key

        // call podExecutorConfig - override secAffinityKey
        result = cicdHelper.podExecutorConfig("jenkins/jnlp-slave:alpine", "0", "jenkins-node-primary", "foo-bar-secondary")
        resultYaml = yaml.load(result)

        assertEquals "jenkins/jnlp-slave:alpine", resultYaml.spec.containers[0].image
        assertEquals 0, resultYaml.spec.securityContext.runAsUser
        assertEquals "jenkins-node-primary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key
        assertEquals "foo-bar-secondary", resultYaml.spec.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[1].preference.matchExpressions[0].key
    }
}

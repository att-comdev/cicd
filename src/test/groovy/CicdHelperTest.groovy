import org.junit.*
import com.lesfurets.jenkins.unit.*
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
}

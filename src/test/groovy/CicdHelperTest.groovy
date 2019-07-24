import org.junit.*
import com.lesfurets.jenkins.unit.*
import static groovy.test.GroovyAssert.*

class CicdHelperTest extends BasePipelineTest {
    def cicdHelper

    @Before
    void setUp() {
        super.setUp()
        // load creds
        cicdHelper = loadScript("vars/cicd_helper.groovy")
    }

    @Test
    void testGetIPMICreds() {

        def yaml = [data: "abc123"]

        // create mock readYaml step
        helper.registerAllowedMethod("readYaml", [ LinkedHashMap ]) { yaml }

        // call getIPMICreds and check result
        def result = cicdHelper.getIPMICreds("nope")
        assertEquals yaml.data, result
    }
}
// Patterns for pipeline parameters in rendered yaml
patterns = [
        'armada/Chart/v1': [
                'mdata': ['ucp-ceph-osd'],
                'data_keys': ['data', 'values', 'conf', 'storage', 'osd'],
                'item_pattern_key': null,
                'item_keys': [],
                'all': true,
                'config_key': 'disks'],
        'pegleg/CommonSoftwareConfig/v1':[
                'mdata': ['common-software-config'],
                'data_keys': ['data', 'osh', 'region_name'],
                'item_pattern': null,
                'all': false,
                'item_keys': [],
                'config_key': 'region_name'],
        'drydock/BaremetalNode/v1' : [
                'mdata': [''],
                'data_keys': ['data', 'addressing'],
                'item_pattern_key': 'network',
                'item_pattern_val': 'oob',
                'item_keys': [],
                'all': true,
                'config_key': 'ipmis'],
        'promenade/KubernetesNetwork/v1': [
                'mdata': ['kubernetes-network'],
                'data_keys': ['data', 'dns', 'service_ip'],
                'item_pattern_key': null,
                'all': false,
                'item_keys': [],
                'config_key': 'dns_service_ip'],
        'pegleg/CommonAddresses/v1':[
                'mdata': ['common-addresses'],
                'data_keys': ['data'],
                'item_pattern': null,
                'all': false,
                'item_keys': [],
                'config_key': 'common_addresses'],
        'pegleg/AccountCatalogue/v1':[
                'mdata': ['aqua_service_accounts'],
                'data_keys': ['data', 'aqua'],
                'item_pattern': null,
                'all': false,
                'item_keys': [],
                'config_key': 'aqua_data'],
        'deckhand/Passphrase/v1':[
                'mdata': ['aqua_executor', 'aqua_orchestrator', 'ucp_shipyard_keystone_password', 'ipmi_admin_password', 'osh_keystone_admin_password'],
                'data_keys': ['data'],
                'item_pattern': null,
                'all': false,
                'item_keys': [],
                'config_key': null],
        'pegleg/SoftwareVersions/v1':[
                'mdata': ['software-versions'],
                'data_keys': ['data', 'images'],
                'item_pattern': null,
                'all': false,
                'item_keys': [],
                'config_key': 'images'],
        'aqua/TestConfig/v1':[
                'mdata': ['aqua-test-configuration'],
                'data_keys': ['data'],
                'item_pattern': null,
                'all': false,
                'item_keys': [],
                'config_key': 'aqua_config'],
        'pegleg/SiteDefinition/v1':[
                'mdata': ["${prefix}"],
                'data_keys': ['data', 'repositories', 'global', 'revision'],
                'item_pattern': null,
                'all': false,
                'item_keys': [],
                'config_key': 'global_ref'],
        'nc/CorridorConfig/v1' : [
                'mdata': ['corridor-config'],
                'data_keys': ['data', 'artifactory', 'hostnames', 'artifacts'],
                'item_pattern': null,
                'all': false,
                'item_keys': [],
                'config_key': 'artifacts'],
]

def readYamlToMap(SITE_NAME) {
    // Load pipeline data from rendered document

    def data = readYaml file: "${SITE_NAME}.yaml"
    def cicd_conf = [:]

    data.each{ yaml ->
        pattern = patterns[yaml.schema]
        if (pattern) {
            pattern.mdata.each { mdata ->
                if (yaml.metadata.name.contains(mdata)) {
                    items = yaml
                    pattern.data_keys.each{ key ->
                        items = items[key]
                    }
                    res = items
                    if ( !(res instanceof List)) {
                        res = [res]
                    }
                    res.each { item ->
                        pattern.item_keys.each { key ->
                            item = item[key]
                        }
                        if (pattern.item_pattern_key) {
                            if (!(item[pattern.item_pattern_key].contains(pattern.item_pattern_val))) {
                                item = null
                            }
                        }
                        config_key = pattern.config_key ? pattern.config_key: mdata
                        if (item) {
                            if ( !(cicd_conf[config_key])) {
                                cicd_conf[config_key] = []
                            }
                            if (pattern.all) {
                                cicd_conf[config_key] << item
                            } else {
                                cicd_conf[config_key] = cicd_conf[config_key] ?: item
                            }
                        }
                    }

                }
            }
        }
    }
	return cicd_conf
}
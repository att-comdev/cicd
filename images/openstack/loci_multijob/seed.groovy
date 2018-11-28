JOB_BASE = 'images/openstack/loci_multijob'
ref = "master"
multijob("complex") {
    parameters {
        stringParam {
            defaultValue(ref)
            description('Default branch for manual build.\n\n' +
                        'Currently master is supported.')
            name ('PROJECTS_BRANCH')
        }
        stringParam {
            defaultValue(ref)
            description('Default reference for manual build.\n\n' +
                        'Branch or gerrit refspec is supported.')
            name ('REQUIREMENTS_REF')
        }
    }
    steps {
        phase("Build requirements image") {
            phaseJob("images/openstack/loci/mos/mos-requirements") {
                parameters {
                    stringParam('PROJECT_REF', "${REQUIREMENTS_REF}")
                }    
            }
        }
    }
}

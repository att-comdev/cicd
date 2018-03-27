base_path = "integration/aql"
 
folder('integration/aql')

pipelineJob("${base_path}/findProperties") {

    parameters {
        stringParam {
            defaultValue('clcp-manifests')
            description('Repository to retrieve properties from')
            name ('REPOSITORY')
        }
        stringParam {
            defaultValue('{"$and": [{"type" : "folder","repo": "$REPOSITORY"}]}')
            description('Find properties at the folder level of Artifactory repository specified')
            name ('AQL_FIND')
        }
        stringParam {
            defaultValue('"property.key","property.value"')
            description('Include Property key and value to the output')
            name ('AQL_INCLUDE')
        }
    }

    triggers {
        definition {
            cps {
                script(readFileFromWorkspace("${base_path}/Jenkinsfile"))
            }
        }
    }
}


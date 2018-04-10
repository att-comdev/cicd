import groovy.json.JsonSlurper

folder("releaseManagement")
folder("releaseManagement/aql")

pipelineJob("releaseManagement/aql/getCurrentArtifacts") {
    parameters {
        stringParam('REPOSITORY',"clcp-manifests")
        stringParam('AQL_INCLUDE',"{"$and": [{"type" : "folder","repo": "$REPOSITORY"}]}")
        stringParam('AQL_FIND',""property.key","property.value"")
    }
    definition {
       cps {
           script(readFileFromWorkspace("releaseManagement/aql/Jenkinsfile"))
           sandbox()
       }
    }
}

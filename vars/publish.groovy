

//This will curl the Jenkins console logs and upload them to Nexus.  
def nexus_logs (String org, String project, String repositoryName) {
// Usage example: publish.nexus_logs('openstack','openstack-helm', 'att-comdev-jenkins-logs')
    sh "curl -s -o ./${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}.log ${BUILD_URL}consoleText"
    nexusArtifactUploader artifacts: [[ artifactId: project,
                                        classifier: '',
                                        file: GERRIT_CHANGE_NUMBER+'-'+GERRIT_PATCHSET_NUMBER+'.log']],
                                        credentialsId: 'nexus3',
                                        groupId: org,
                                        nexusUrl: '$NEXUS3_URL',
                                        nexusVersion: 'nexus3',
                                        protocol: 'http',
                                        repository: repositoryName,
                                        version: '$BUILD_NUMBER'
}

def artifactory_images () {
// Usage example: publish.nexus_logs('openstack','openstack-helm', 'att-comdev-jenkins-logs')
    sh "curl -s -o ./${GERRIT_CHANGE_NUMBER}-${GERRIT_PATCHSET_NUMBER}.log ${BUILD_URL}consoleText"
    nexusArtifactUploader artifacts: [[ artifactId: project,
                                        classifier: '',
                                        file: GERRIT_CHANGE_NUMBER+'-'+GERRIT_PATCHSET_NUMBER+'.log']],
                                        credentialsId: 'nexus3',
                                        groupId: org,
                                        nexusUrl: '$NEXUS3_URL',
                                        nexusVersion: 'nexus3',
                                        protocol: 'http',
                                        repository: repositoryName,
                                        version: '$BUILD_NUMBER'
}


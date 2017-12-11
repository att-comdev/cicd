JOB_FOLDER='images'
JOB_NAME='kubernetes'

folder(JOB_FOLDER)
pipelineJob("${JOB_FOLDER}/${JOB_NAME}") {
    parameters {
        stringParam {
            name ('RELEASE_BRANCH')
            defaultValue('release-1.8')
            description('Kube release branch')
        }
    }
    scm {
        github('nycmoma/4jenkins', "${RELEASE_BRANCH}")
    }
    triggers {
        scm('*/5 * * * *')
//        gitHubPushTrigger()
   }
    definition {
        cps {
            script(readFileFromWorkspace("${JOB_FOLDER}/${JOB_NAME}/Jenkinsfile"))
            sandbox()
        }
    }
}

//freeStyleJob("${JOB_FOLDER}/${JOB_NAME}") {
//    parameters {
//        stringParam {
//            name ('RELEASE_BRANCH')
//            defaultValue('release-1.8')
//            description('Kube release branch')
//        }
//    }
//    scm {
//        github('nycmoma/4jenkins', 'release-1.8')
//    }
//    triggers {
//        githubPush()
//    }
//    steps {
//        slack.msg('It works!')
//    }
//    publishers {
//        archiveArtifacts('job-dsl-plugin/build/libs/job-dsl.hpi')
//    }
//}

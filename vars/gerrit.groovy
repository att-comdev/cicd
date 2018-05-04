import att.comdev.cicd.gerrit.Setup

def checkoutToRelativeDir(Setup setup, repo, targetDir, branch = "master", refspec = "master") {
    checkout poll: false,
    scm: [$class: "GitSCM",
        branches: [[name: "${branch}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: "RelativeTargetDirectory",
            relativeTargetDir: "${targetDir}"]],
        submoduleCfg: [],
        userRemoteConfigs: [[refspec: "${refspec}",
                             url: "ssh://" + setup.repoUser + "@" + setup.gerritUrl + "/${repo}",
                             credentialsId: setup.credId]]]
}
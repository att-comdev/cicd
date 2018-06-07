/**
 * This is a wrapper library for vars/gerrit.groovy for use with gerrithub
 * TODO: remove it completely
*/

def clone(String project, String refspec){
// Usage example: gerrithub.clone("att-comdev/cicd", "origin/master")
// clone refspec: gerrithub.clone("att-comdev/cicd", "${env.GERRIT_REFSPEC}")
  gerrit.clone("https://review.gerrithub.io/" + project, refspec)
}

def cloneToBranch(String project, String refspec, String targetDirectory){
//This method is used so that we can checkout the patchset to a local
//branch and then rebase it locally with the current master before we build and test
  gerrit.cloneToBranch("https://review.gerrithub.io/" + project, refspec, targetDirectory)
}

def cloneProject(String project, String branch, String refspec, String targetDirectory){
//This method is used so that we can checkout different project
//from any patchset in different pipelines
// Usage example: gerrithub.cloneProject("att-comdev/cicd", "*/master", "refs/XX/XX/XX" "cicd")
  gerrit.cloneProject("https://review.gerrithub.io/" + project, branch, refspec, targetDirectory)
}

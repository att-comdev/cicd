
def git_branch = GIT_BRANCH
def git_project = GIT_REPO + '-' + GIT_PROJECT

def job_name = git_project + '/' + git_branch + '/' + 'seed'

folder(git_project){
    displayName(git_project)
}

folder(git_project+'/'+git_branch){
    displayName(git_branch)
}

listView(git_project) {
    description('OpenStack-Helm CI')
    jobs {
        name(git_project)
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

job(job_name) {
    parameters {
        stringParam('GIT_PROJECT', GIT_PROJECT)
        stringParam('GIT_REPO', GIT_REPO)
        stringParam('GIT_URL', GIT_URL)
        stringParam('GIT_REFSPEC', GIT_REFSPEC)
        stringParam('GIT_BRANCH', GIT_BRANCH)
        stringParam('SLACK_TEAM', SLACK_TEAM)
        stringParam('SLACK_TOKEN', SLACK_TOKEN)
        stringParam('SLACK_ROOM', SLACK_ROOM)
    }
    triggers {
        gerrit {
            events {
                changeMerged()
                draftPublished()
                patchsetCreated()
                refUpdated()
            }
            project(GIT_REPO+'/'+GIT_PROJECT, GIT_BRANCH)
            buildSuccessful(10, null)
        }
    }
    steps {
        dsl {
            external 'osh/openstack/openstack-single-node/*_job.groovy'
            additionalClasspath 'osh/openstack/openstack-single-node/groovy'
        }
    }
}

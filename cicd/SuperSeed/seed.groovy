base_path = "cicd"
job_path = "${base_path}/SuperSeed"
folder("${base_path}")

superseed_script = '''

set -xe

git_clone(){

    local project_name=$1
    local local_path=$2
    local refspec=$3
    local gerrit_url='http://gerrithub.io'
    #local gerrit_url='ssh://jenkins@gerrithub.io:29418'

    if [[ "${local_path}" =~ ^[\/\.]*$ ]]; then
        echo "ERROR: Bad local path '${local_path}'"
        exit 1
    fi

    if [ -z "${refspec}" ]; then
        echo "ERROR: Empty refspec given"
        exit 1
    fi

    git clone ${gerrit_url}/${project_name} ${local_path}

    pushd ${local_path}
    if [[ "${refspec}" =~ ^refs\/ ]]; then
        git fetch ${gerrit_url}/${project_name} ${refspec}
        git checkout FETCH_HEAD
    else
        git checkout ${refspec}
    fi
    popd
}

copy_seed(){

    if [ -f "${WORKSPACE}/$1" ]; then
        echo "INFO: Seed file found! $1 "
        mkdir -p ${WORKSPACE}/${BUILD_NUMBER}
        # "${BUILD_NUMBER}/seed.groovy" is a hardcoded path
        # for 'Process Job DSLs' part of the job.
        # See cicd/SuperSeed/seed.groovy file.
        cp -a ${WORKSPACE}/$1 ${WORKSPACE}/${BUILD_NUMBER}/seed.groovy
    else
        #Fail the build if file doesn't exists:
        echo "ERROR: No seed files found"
        exit 1
    fi
}

find_seed(){

    if [ -z "${SEED_PATH}" ]; then
        if [ "${GERRIT_REFSPEC}" = "origin/master" ]; then
            echo "ERROR: empty SEED_PATH parameter."
            exit 1
        fi

        echo "INFO: Looking for seed.groovy file in refspec changes..."
        LAST_2COMMITS=`git log -2 --reverse --pretty=format:%H`

        #Looking for added or modified seed.groovy files or Jenkinsfiles:
        MODIFIED_FILES=`git diff --name-status ${LAST_2COMMITS} | grep -v ^D | grep 'seed.groovy' | cut -f2`
        echo "INFO: changed seed file: $MODIFIED_FILES"
        if [ -z "${MODIFIED_FILES}" ]; then
            #No seeds?, looking for modified Jenkinsfile files:
            MODIFIED_FILES=`git diff --name-status ${LAST_2COMMITS} | grep -v ^D | grep Jenkinsfile| cut -f2`
        echo "INFO: changed Jenkinsfile: $MODIFIED_FILES"
        fi

        if [ ! -z "${MODIFIED_FILES}" ]; then
            for file in ${MODIFIED_FILES}; do
                SEED_PATH=$(dirname ${file})/seed.groovy
                export SEED_PATH=${SEED_PATH}
            done
        fi

    else
        #if SEED_PATH param is not empty, we don't need to do anything:
        echo ${SEED_PATH}
    fi
}

######MAIN#####
git_clone ${GERRIT_PROJECT} ${WORKSPACE} ${GERRIT_REFSPEC}
find_seed
set +x
copy_seed ${SEED_PATH}

#Empty space for DSL script debug information:
echo -e "=================================================\n"
'''

freeStyleJob("${job_path}") {
    label('master')
    parameters {
        stringParam {
            name ('SEED_PATH')
            defaultValue('')
            description('Seed path. Example: cicd/SuperSeed/seed.groovy')
        }
        stringParam {
            name ('GERRIT_REFSPEC')
            defaultValue('origin/master')
            description('Gerrit refspec')
        }
        stringParam {
            name ('GERRIT_PROJECT')
            defaultValue('att-comdev/cicd')
            description('Project on Gerrithub')
        }
    }

    triggers {
        gerritTrigger {
            serverName('Gerrithub-jenkins')
            gerritProjects {
                gerritProject {
                    compareType('PLAIN')
                    pattern("att-comdev/cicd")
                    branches {
                        branch {
                            compareType('ANT')
                            pattern("**/master")
                        }
                    }
                    disableStrictForbiddenFileVerification(false)
                }
            }
            triggerOnEvents {
/// PatchsetCreated trigger should be manually enabled on staging:
//                patchsetCreated {
//                   excludeDrafts(true)
//                   excludeTrivialRebase(false)
//                   excludeNoCodeChange(false)
//                }

/// changeMerged trigger for production:
            changeMerged()
            }
        }
    }
    steps {
    //Wipe the workspace:
        wrappers {
            preBuildCleanup()
        }
        sh superseed_script
        dsl {
            external('${BUILD_NUMBER}/seed.groovy')
            //ignoreExisting(true)
            removeAction('DISABLE')
        }
    }
}

pipelineJob("integration/uplift-versions/${entry.repo_name}") {
	disabled(false)
	logRotator{
		daysToKeep(90)
	}
	parameters {
		stringParam('NODE_POSTFIX', "user-vm1")
		stringParam('CICD_REFSPEC', "refs/changes/53/432353/15")
	}
	triggers {
		definition {
			cps {
				script(readFileFromWorkspace('integration/uplift-versions/Jenkinsfile'))
				sandbox(false)
			}
		}
	}
}
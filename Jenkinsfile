#!groovy

def workerNode = "devel10"

pipeline {
	agent {label workerNode}
	tools {
		jdk 'jdk11'
		maven 'Maven 3'
	}
	triggers {
		upstream(upstreamProjects: "Docker-payara5-bump-trigger",
            threshold: hudson.model.Result.SUCCESS)
	}
	options {
		timestamps()
		disableConcurrentBuilds()
	}
	stages {
		stage("clear workspace") {
			steps {
				deleteDir()
				checkout scm
			}
		}
		stage("verify") {
			steps {
				sh "mvn -D sourcepath=src/main/java verify pmd:pmd javadoc:aggregate"

				// We don't have any tests yet

				//junit testResults: '**/target/*-reports/TEST-*.xml'

				script {
					def java = scanForIssues tool: [$class: 'Java']
					def javadoc = scanForIssues tool: [$class: 'JavaDoc']
					publishIssues issues: [java, javadoc]

					def pmd = scanForIssues tool: [$class: 'Pmd'], pattern: '**/target/pmd.xml'
					publishIssues issues: [pmd]

					// spotbugs still has some outstanding issues with regard
					// to analyzing Java 11 bytecode.

					//def spotbugs = scanForIssues tool: [$class: 'SpotBugs'], pattern: '**/target/spotbugsXml.xml'
					//publishIssues issues:[spotbugs]
				}
			}
		}
		stage("docker push") {
			when {
                branch "master"
            }
			steps {
				script {
					docker.image("docker-io.dbc.dk/promat-service:${env.BRANCH_NAME}-${env.BUILD_NUMBER}").push()
				}
			}
		}
	}
}

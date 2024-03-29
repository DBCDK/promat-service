#!groovy

def workerNode = "devel11"

pipeline {
	agent {label workerNode}

	tools {
		jdk 'jdk11'
		maven 'Maven 3'
	}

    environment {
		GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
	}
  triggers {
    upstream('/Docker-payara6-bump-trigger')
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

				junit testResults: '**/target/*-reports/TEST-*.xml'

				script {
					def java = scanForIssues tool: [$class: 'Java']
					def javadoc = scanForIssues tool: [$class: 'JavaDoc']
					publishIssues issues: [java, javadoc]

					def pmd = scanForIssues tool: [$class: 'Pmd'], pattern: '**/target/pmd.xml'
					publishIssues issues: [pmd]

					// spotbugs still has some outstanding issues with regard
					// to analyzing Java 11 bytecode.

					def spotbugs = scanForIssues tool: [$class: 'SpotBugs'], pattern: '**/target/spotbugsXml.xml'
					publishIssues issues:[spotbugs]
				}
			}
		}

		stage("docker push") {
			when {
                branch "master"
            }
			steps {
				script {
					docker.image("docker-metascrum.artifacts.dbccloud.dk/promat-service:${env.BRANCH_NAME}-${env.BUILD_NUMBER}").push()
				}
			}
		}

        stage("bump docker tag in promat-service-secrets") {
            agent {
                docker {
                    label workerNode
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                branch "master"
            }
            steps {
                script {
                    sh """
                        set-new-version services/promat-service.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/promat-service-secrets  ${env.BRANCH_NAME}-${env.BUILD_NUMBER} -b staging
                    """
                }
            }
        }

        stage("deploy to maven repository") {
            when {
                branch "master"
            }
            steps {
                sh """
                    mvn deploy -Dmaven.test.skip=true -am -pl model -pl connector
                """
            }
        }
    }
}

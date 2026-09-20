#!groovy

@Library('dependency-track')
// Temporarily pinned to the fix-hardcoded-resource-type branch to test the kind-aware
// delete + rollout-wait fix before merging to main and cutting a new release. Revert to
// a tagged release (e.g. team-x-tools@1.2.0, or whatever supersedes it) once verified.
@Library('team-x-tools@fix-hardcoded-resource-type')

def workerNode = "devel12"
def teamSlackNotice = 'de-notifications'
def teamSlackWarning = 'de-notifications'
def featureNamespace = 'promat-features'
def kubeconfigCredentialsId = 'kubecert-team-x'
def postgresImage = 'docker-dbc.artifacts.dbccloud.dk/dbc-postgres-17:latest'

pipeline {
	agent {label workerNode}

	tools {
		maven 'Maven 3'
	}

	environment {
		IMAGE = "docker-metascrum.artifacts.dbccloud.dk/promat-service:${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
	}

  triggers {
    pollSCM("*/3 8-16  * *  *")
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
		stage("sonarqube") {
            steps {
                withSonarQubeEnv(installationName: 'sonarqube.dbc.dk') {
                    script {
                        def status = 0

                        def sonarOptions = "-Dsonar.branch.name=${BRANCH_NAME}"
                        if (env.BRANCH_NAME != 'master') {
                            sonarOptions += " -Dsonar.newCode.referenceBranch=master"
                        }

                        // Do sonar via maven
                        status += sh returnStatus: true, script: """
                            mvn -B $sonarOptions sonar:sonar
                        """

                        if (status != 0) {
                            error("build failed")
                        }
                    }
                }
            }
        }
        stage("quality gate") {
            steps {
                // wait for analysis results
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
		stage("verify") {
			steps {
				sh "mvn -D sourcepath=src/main/java verify pmd:pmd"

				junit testResults: '**/target/*-reports/TEST-*.xml'

				script {
					def java = scanForIssues tool: [$class: 'Java']
					def javadoc = scanForIssues tool: [$class: 'JavaDoc']
					publishIssues issues: [java, javadoc]

					def pmd = scanForIssues tool: [$class: 'Pmd'], pattern: '**/target/pmd.xml'
					publishIssues issues: [pmd]
				}
			}
		}
		stage("supply-chain gate") {
            steps {
                script {
                    dependencyTrackGate(
                        projectBom:  'target/sbom-java.json',
                        projectTeam: 'de-team',
                        projectType: 'java',
                    )
                }
            }
        }
		stage("docker push") {
			steps {
				script {
					docker.image(IMAGE).push()
				}
			}
		}
        stage("Deploy feature branch") {
            when {
                not { branch "master" }
            }
            steps {
                script {
                    def dbUser = 'promat'
                    def dbName = 'promat_db'
                    def dbPassword = UUID.randomUUID().toString().replaceAll('-', '')

                    // Disposable feature preview only: the feature deployment tool labels and
                    // cleans these temporary resources when this branch/build is superseded.
                    // There is deliberately no PVC, so the preview database disappears with the pod.
                    def dbPreviewUrl = gitopsSecretsFeatureBranch(
                        sourceNamespace: featureNamespace,
                        manifest: 'promat-service/promat-service-db.yml',
                        image: postgresImage,
                        kubeconfigCredentialsId: kubeconfigCredentialsId,
                        envOverrides: [
                            POSTGRES_PASSWORD: dbPassword,
                        ],
                    )
                    def dbHost = dbPreviewUrl.replace('http://', '')

                    // Feature branch deployment of the service itself.
                    env.PREVIEW_URL = gitopsSecretsFeatureBranch(
                        sourceNamespace: featureNamespace,
                        manifest: 'promat-service/promat-service.yml',
                        image: IMAGE,
                        kubeconfigCredentialsId: kubeconfigCredentialsId,
                        envOverrides: [
                            PROMAT_DB_URL: "${dbUser}:${dbPassword}@${dbHost}:5432/${dbName}",
                        ],
                    )

                    // Seed data is just for the disposable preview/local workflow. It is not part
                    // of app startup or Flyway migrations, and the script refuses non-preview hosts.
                    docker.image(postgresImage).inside {
                        sh """
                            set +x
                            scripts/feature-preview/seed \
                                --host '${dbHost}' \
                                --port 5432 \
                                --dbname '${dbName}' \
                                --dbuser '${dbUser}' \
                                --dbpassword '${dbPassword}'
                        """
                    }
                    echo "Deployed preview: ${env.PREVIEW_URL}"
                }
            }
        }
        stage("Update staging version number") {
            when {
                branch "master"
            }
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: "gitlab-isworker", keyFileVariable: "sshkeyfile")]) {
                        env.GIT_SSH_COMMAND = "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i ${sshkeyfile}"
                        sh """
                            nix run --refresh git+https://gitlab.dbc.dk/public-de-team/gitops-secrets-set-variables.git \
                                metascrum-staging:PROMAT_SERVICE_VERSION=${BRANCH_NAME}-${BUILD_NUMBER}
                        """
                    }
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
    post {
        success {
            script {
                if (BRANCH_NAME == 'master') {
                    slackSend(channel: teamSlackNotice,
                            color: 'good',
                            message: "${JOB_NAME} #${BUILD_NUMBER} completed, and pushed to artifactory.",
                            tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
        fixed {
            script {
                if ("${env.BRANCH_NAME}" == 'master') {
                    slackSend(channel: teamSlackWarning,
                            color: 'good',
                            message: "${env.JOB_NAME} #${env.BUILD_NUMBER} back to normal: ${env.BUILD_URL}",
                            tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
        failure {
            script {
                if ("${env.BRANCH_NAME}".equals('master')) {
                    slackSend(channel: teamSlackWarning,
                        color: 'warning',
                        message: "${env.JOB_NAME} #${env.BUILD_NUMBER} failed and needs attention: ${env.BUILD_URL}",
                        tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
    }
}

pipeline {
    agent any

    tools {
        jdk 'JDK 17'
        maven 'Maven 3.9.6'
    }

    options {
        skipStagesAfterUnstable()
    }

    environment {
        JAR_NAME = "social-service-1.0.0.jar"
        REMOTE_DIR = "/home/ec2-user/revhub/jars"
        APP_PORT = "8082"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B clean compile'
            }
            post {
                success {
                    echo 'Build successful'
                }
                failure {
                    echo 'Build failed'
                }
            }
        }

        stage('Test') {
            steps {
                bat 'mvn -B test'
            }
            post {
                success {
                    echo 'Test successful'
                }
                failure {
                    echo 'Test failed'
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B package -DskipTests'
            }
            post {
                success {
                    echo "JAR CREATED SUCCESSFULLY"
                }
            }
        }

        // stage('SonarQube Analysis') {
        //     steps {
        //         withSonarQubeEnv('SonarCloud') {
        //             bat 'mvn sonar:sonar'
        //         }
        //     }
        // }

        stage("Deploy to EC2") {
            steps {
                sshPublisher(
                    publishers: [
                        sshPublisherDesc(
                            configName: "revhub-services",
                            verbose: true,
                            transfers: [
                                sshTransfer(
                                    sourceFiles: "target/${JAR_NAME}",
                                    removePrefix: "target/",
                                    remoteDirectory: "",
                                    flatten: true,
                                    execCommand: "ls -la ${REMOTE_DIR}"
                                )
                            ]
                        )
                    ]
                )
            }
        }
    }

    post {
        always {
            echo "Pipeline Done..."
            cleanWs()
        }
    }
}

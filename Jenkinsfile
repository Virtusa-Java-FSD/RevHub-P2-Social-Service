pipeline {
    agent any
    
    tools {
        maven 'Maven 3.9.6' 
        jdk 'JDK 17' 
    }
    
    stages {
        stage('Build') {
            steps {
                bat 'mvn clean compile'
            }
        }
        
        stage('Test') {
            steps {
                bat 'mvn test'
            }
        }
        
        stage('Package') {
            steps {
                bat 'mvn package -DskipTests'
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline Job Succeeded! ✅'
        }
        failure {
            echo 'Pipeline Job Failed! ❌'
        }
    }
}

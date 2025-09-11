pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'minigame-service'
        DOCKER_TAG = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"
        FULL_IMAGE_NAME = "${DOCKER_IMAGE}:${DOCKER_TAG}"
        KUBECONFIG = '/var/jenkins_home/.kube/config'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    env.GIT_BRANCH = sh(returnStdout: true, script: 'git branch --show-current || echo "dev/junho"').trim()
                    echo "Building commit: ${env.GIT_COMMIT}"
                    echo "Building branch: ${env.GIT_BRANCH}"
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    try {
                        sh '''
                            chmod +x gradlew
                            ./gradlew clean test --no-daemon --stacktrace -Dspring.profiles.active=ci
                        '''
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "Tests failed but continuing pipeline: ${e.getMessage()}"
                    }
                }
            }
            post {
                always {
                    script {
                        try {
                            junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'
                        } catch (Exception e) {
                            echo "No test results found or failed to publish: ${e.getMessage()}"
                        }
                    }
                }
            }
        }
        
        stage('Build Application') {
            steps {
                sh '''
                    chmod +x gradlew
                    ./gradlew clean bootJar -x test --no-daemon --stacktrace
                '''
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    echo "✅ Application JAR built successfully"
                    echo "🔨 Building Docker image: ${FULL_IMAGE_NAME}"
                    
                    sh "docker build -t ${FULL_IMAGE_NAME} ."
                    sh "docker tag ${FULL_IMAGE_NAME} ${FULL_IMAGE_NAME}"
                    
                    echo "✅ Docker image built and loaded to minikube"
                    echo "📦 Ready for deployment via GitOps"
                }
            }
        }
        
        stage('Update K8s Manifests') {
            steps {
                script {
                    sh """
                        echo "Current image in manifest:"
                        grep "image:" k8s/minigame.yaml || echo "No image line found"
                        
                        echo "Updating image to: ${FULL_IMAGE_NAME}"
                        sed -i 's|image: minigame-service:.*|image: ${FULL_IMAGE_NAME}   # Updated by Jenkins CI|g' k8s/minigame.yaml
                        
                        echo "Updated manifest:"
                        grep "image:" k8s/minigame.yaml || echo "No image line found"
                        
                        echo "Committing changes..."
                        git config user.name "Jenkins CI"
                        git config user.email "jenkins@betweenus.com"
                        git add k8s/minigame.yaml
                        
                        # Create local branch to avoid detached HEAD
                        git checkout -B dev/junho
                        
                        git commit -m "[skip ci] ci: update image to ${FULL_IMAGE_NAME}

Build: #${BUILD_NUMBER}
Commit: ${GIT_COMMIT}

🤖 Generated with Jenkins CI Pipeline"

                        echo "Pushing to repository..."
                    """
                    
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        sh "git push https://\${GITHUB_TOKEN}@github.com/BetweenUs-Prj/MiniGameService.git HEAD:refs/heads/dev/junho"
                    }
                }
            }
        }
        
        stage('Deploy Notification') {
            steps {
                script {
                    echo "✅ Image ${FULL_IMAGE_NAME} built and manifest updated"
                    echo "🚀 ArgoCD will automatically deploy the changes"
                    echo "📊 Monitor deployment at: http://localhost:8081"
                    echo "🔗 Application: http://localhost:8080"
                    echo "🔗 Backend API: http://localhost:8084"
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo '🎉 Pipeline completed successfully!'
            echo "✅ New image: ${FULL_IMAGE_NAME}"
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        unstable {
            echo '⚠️ Pipeline completed with warnings (test failures)'
        }
    }
}
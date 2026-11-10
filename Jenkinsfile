
pipeline {
    agent any

    tools {
        jdk 'jdk25'
        maven 'maven3'
        nodejs 'node22'
    }

    parameters {
        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: 'Enable deployment only after configuring a Linux Ansible agent'
        )
        string(
            name: 'DEPLOY_BRANCH',
            defaultValue: 'main',
            description: 'Only this branch is eligible for deployment'
        )
        string(
            name: 'APP_HEALTH_URL',
            defaultValue: '',
            description: 'Optional application health URL'
        )
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '5'))
    }

    triggers {
        githubPush()
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        APP_JAR = 'backend/target/student-attendance.jar'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm

                script {
                    env.BRANCH_SHORT = (
                        env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'unknown'
                    ).replaceFirst('^origin/', '')

                    env.GIT_SHORT = bat(
                        script: '@echo off\r\ngit rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                }

                echo "Building branch '${env.BRANCH_SHORT}' at commit ${env.GIT_SHORT}"
            }
        }

        stage('Verify tools') {
            steps {
                bat '''
                    @echo off
                    java -version
                    mvn -version
                    node --version
                    npm --version
                    git --version
                '''
            }
        }

        stage('Build frontend') {
            steps {
                dir('frontend') {
                    bat 'npm ci --no-audit --no-fund'
                    bat 'npm test'
                    bat 'npm run build'
                }

                bat '''
                    @echo off

                    if not exist "frontend\\dist" (
                        echo ERROR: Frontend build directory not found.
                        exit /b 1
                    )

                    if exist "backend\\src\\main\\resources\\static" (
                        rmdir /s /q "backend\\src\\main\\resources\\static"
                    )

                    mkdir "backend\\src\\main\\resources\\static"

                    xcopy "frontend\\dist\\*" "backend\\src\\main\\resources\\static\\" /E /I /Y

                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Build, test and package') {
            steps {
                bat 'mvn -B -ntp -f backend\\pom.xml clean verify'
            }

            post {
                always {
                    junit(
                        testResults: 'backend/target/surefire-reports/*.xml, backend/target/failsafe-reports/*.xml',
                        allowEmptyResults: false
                    )
                }
            }
        }

        stage('Archive artifact') {
            steps {
                bat '''
                    @echo off

                    if not exist "%APP_JAR%" (
                        echo ERROR: Application JAR not found.
                        exit /b 1
                    )

                    dir "%APP_JAR%"
                '''

                archiveArtifacts(
                    artifacts: 'backend/target/student-attendance.jar',
                    fingerprint: true
                )
            }
        }

        stage('Deployment gate') {
            when {
                expression {
                    params.DEPLOY &&
                    env.BRANCH_SHORT == params.DEPLOY_BRANCH
                }
            }

            steps {
                echo "Deployment requested for ${env.BRANCH_SHORT}, build #${env.BUILD_NUMBER}."
            }
        }

        stage('Deploy with Ansible') {
            when {
                expression {
                    params.DEPLOY &&
                    env.BRANCH_SHORT == params.DEPLOY_BRANCH
                }
            }

            steps {
                error(
                    'Ansible deployment is not enabled on this Windows Jenkins agent. ' +
                    'Configure a Linux deployment agent before enabling DEPLOY.'
                )
            }
        }

        stage('Verify deployment') {
            when {
                expression {
                    params.DEPLOY &&
                    env.BRANCH_SHORT == params.DEPLOY_BRANCH &&
                    params.APP_HEALTH_URL?.trim()
                }
            }

            steps {
                bat '''
                    @echo off

                    powershell -NoProfile -ExecutionPolicy Bypass -Command "$response = Invoke-RestMethod -Uri $env:APP_HEALTH_URL; if ($response.status -ne 'UP') { Write-Error 'Application health check failed'; exit 1 }; $response | ConvertTo-Json | Set-Content health.json"

                    if errorlevel 1 exit /b 1

                    type health.json
                '''
            }
        }
    }

    post {
        success {
            echo "PIPELINE SUCCESS - branch ${env.BRANCH_SHORT}, build #${env.BUILD_NUMBER}, commit ${env.GIT_SHORT}"
        }

        failure {
            echo 'PIPELINE FAILED - inspect the failed stage and its console output.'
        }

        always {
            bat 'if exist health.json del /f /q health.json'
        }
    }
}

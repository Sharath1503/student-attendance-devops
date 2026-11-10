
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
            description: 'Deploy the application to Ubuntu WSL using Ansible'
        )
        string(
            name: 'DEPLOY_BRANCH',
            defaultValue: 'main',
            description: 'Only this branch is eligible for deployment'
        )
        string(
            name: 'APP_HEALTH_URL',
            defaultValue: 'http://localhost:8081/actuator/health',
            description: 'Application health endpoint'
        )
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(
            numToKeepStr: '15',
            artifactNumToKeepStr: '5'
        ))
    }

    triggers {
        githubPush()
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        APP_JAR = 'backend\\target\\student-attendance.jar'

        SSH_HOST = '172.28.51.54'
        SSH_USER = 'sande'
        SSH_KEY = 'C:\\ProgramData\\Jenkins\\ssh\\jenkins_wsl_key'
        SSH_KNOWN_HOSTS = 'C:\\ProgramData\\Jenkins\\.jenkins\\.ssh\\known_hosts'
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
                    ssh -V
                    scp
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
                echo 'Transferring application JAR to Ubuntu WSL.'

                bat '''
                    @echo off

                    scp -i "%SSH_KEY%" ^
                        -o BatchMode=yes ^
                        -o StrictHostKeyChecking=yes ^
                        -o UserKnownHostsFile="%SSH_KNOWN_HOSTS%" ^
                        "%WORKSPACE%\\backend\\target\\student-attendance.jar" ^
                        %SSH_USER%@%SSH_HOST%:/tmp/student-attendance.jar

                    if errorlevel 1 exit /b 1
                '''

                echo 'Preparing release version and executing Ansible deployment.'

                bat '''
                    @echo off

                    ssh -i "%SSH_KEY%" ^
                        -o BatchMode=yes ^
                        -o StrictHostKeyChecking=yes ^
                        -o UserKnownHostsFile="%SSH_KNOWN_HOSTS%" ^
                        %SSH_USER%@%SSH_HOST% ^
                        "printf 'app_jar_src: /tmp/student-attendance.jar\\napp_version: jenkins-%BUILD_NUMBER%\\n' > /tmp/deploy-vars.yml && sudo -n /usr/local/sbin/deploy-attendance"

                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Verify deployment') {
            when {
                expression {
                    params.DEPLOY &&
                    env.BRANCH_SHORT == params.DEPLOY_BRANCH
                }
            }

            steps {
                echo "Checking application health at ${params.APP_HEALTH_URL}"

                bat '''
                    @echo off

                    powershell -NoProfile -ExecutionPolicy Bypass -Command "$response = Invoke-RestMethod -Uri '%APP_HEALTH_URL%' -TimeoutSec 30; if ($response.status -ne 'UP') { throw 'Application health check failed' }; Write-Output 'Application health: UP'"

                    if errorlevel 1 exit /b 1
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

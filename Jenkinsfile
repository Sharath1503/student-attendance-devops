// Declarative pipeline for the Online Student Attendance System.
// Flow: Checkout -> Tool check -> Frontend build -> Maven build+test+package -> Archive -> (main only) Ansible deploy -> Health check
//
// Jenkins prerequisites (see docs/DEVOPS_SETUP_GUIDE.md):
//   Global Tool Configuration : JDK named "jdk21", Maven named "maven3", NodeJS named "node22" (NodeJS plugin)
//   Credentials (all optional until you deploy):
//     attendance-deploy-ssh   SSH Username with private key   (deploy user on the target server)
//     attendance-inventory    Secret file                     (your ansible inventory.ini, never committed)
//     attendance-db-password  Secret text                     (MySQL password of the application account)
//     attendance-jwt-secret   Secret text                     (>= 32 random characters)
//     attendance-admin-password Secret text                   (initial administrator password)
//   Agent needs: git, ansible-playbook (only for the deploy stages), curl

pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
        nodejs 'node22'
    }

    parameters {
        booleanParam(name: 'DEPLOY', defaultValue: true,
                description: 'Deploy after a successful build (only honoured on the deploy branch)')
        string(name: 'DEPLOY_BRANCH', defaultValue: 'main',
                description: 'Only this branch is ever deployed. Feature branches are built and tested only.')
        string(name: 'APP_HEALTH_URL', defaultValue: '',
                description: 'Optional extra check from Jenkins, e.g. http://my-server:8080/actuator/health')
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15', artifactNumToKeepStr: '5'))
    }

    // GitHub webhook (needs the GitHub plugin and a webhook pointing at <jenkins>/github-webhook/).
    // Polling fallback if webhooks cannot reach Jenkins: replace with  pollSCM('H/5 * * * *')
    triggers {
        githubPush()
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        APP_JAR    = 'backend/target/student-attendance.jar'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.BRANCH_SHORT = (env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'unknown').replaceFirst('^origin/', '')
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                }
                echo "Building branch '${env.BRANCH_SHORT}' at commit ${env.GIT_SHORT}"
            }
        }

        stage('Verify tools') {
            steps {
                sh '''
                    set -e
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
                    sh 'npm ci --no-audit --no-fund'
                    sh 'npm test'
                    sh 'npm run build'
                }
                // Spring Boot serves the React build, so the deployable is ONE jar.
                sh '''
                    set -e
                    rm -rf backend/src/main/resources/static
                    mkdir -p backend/src/main/resources/static
                    cp -r frontend/dist/. backend/src/main/resources/static/
                '''
            }
        }

        stage('Build, test and package') {
            // "mvn verify" = compile -> unit tests -> package jar -> integration tests.
            // Any compile error or failing test fails this stage, so nothing after it runs (no deployment).
            steps {
                sh 'mvn -B -ntp -f backend/pom.xml clean verify'
            }
            post {
                always {
                    junit testResults: 'backend/target/surefire-reports/*.xml, backend/target/failsafe-reports/*.xml',
                          allowEmptyResults: false
                }
            }
        }

        stage('Archive artifact') {
            steps {
                sh '''
                    set -e
                    test -f "$APP_JAR"
                    ls -l "$APP_JAR"
                '''
                archiveArtifacts artifacts: 'backend/target/student-attendance.jar', fingerprint: true
            }
        }

        stage('Deployment gate') {
            when {
                expression { params.DEPLOY && env.BRANCH_SHORT == params.DEPLOY_BRANCH }
            }
            steps {
                echo "Branch '${env.BRANCH_SHORT}' is the deploy branch - deploying build #${env.BUILD_NUMBER}."
            }
        }

        stage('Deploy with Ansible') {
            when {
                expression { params.DEPLOY && env.BRANCH_SHORT == params.DEPLOY_BRANCH }
            }
            steps {
                // Secrets are injected as environment variables and read by Ansible via lookup('env', ...).
                // They are never put on a command line, and the single-quoted sh block keeps Groovy from expanding them.
                withCredentials([
                    sshUserPrivateKey(credentialsId: 'attendance-deploy-ssh', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER'),
                    file(credentialsId: 'attendance-inventory', variable: 'INVENTORY_FILE'),
                    string(credentialsId: 'attendance-db-password', variable: 'DB_PASSWORD'),
                    string(credentialsId: 'attendance-jwt-secret', variable: 'JWT_SECRET'),
                    string(credentialsId: 'attendance-admin-password', variable: 'BOOTSTRAP_ADMIN_PASSWORD')
                ]) {
                    sh '''
                        set -e
                        export ANSIBLE_CONFIG="$WORKSPACE/ansible/ansible.cfg"
                        ansible-playbook \
                          -i "$INVENTORY_FILE" \
                          --private-key "$SSH_KEY" -u "$SSH_USER" \
                          -e "app_jar_src=$WORKSPACE/$APP_JAR" \
                          -e "app_version=${BUILD_NUMBER}-${GIT_SHORT}" \
                          ansible/playbooks/deploy.yml
                    '''
                }
            }
        }

        stage('Verify deployment') {
            when {
                expression { params.DEPLOY && env.BRANCH_SHORT == params.DEPLOY_BRANCH && params.APP_HEALTH_URL?.trim() }
            }
            steps {
                // Ansible has already checked health on the server; this proves it is reachable from Jenkins too.
                sh '''
                    set -e
                    curl --fail --silent --show-error --retry 10 --retry-delay 3 --retry-connrefused "$APP_HEALTH_URL" | tee health.json
                    grep -q '"status":"UP"' health.json
                '''
            }
        }
    }

    post {
        success {
            echo "PIPELINE SUCCESS - branch ${env.BRANCH_SHORT}, build #${env.BUILD_NUMBER}, commit ${env.GIT_SHORT}"
        }
        failure {
            echo 'PIPELINE FAILED - open the failed stage above. Nothing after a failed build/test stage is executed, so no deployment happened from this run.'
        }
        always {
            sh 'rm -f health.json'
        }
    }
}

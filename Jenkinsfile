pipeline {
environment {
        BRANCH = 1.11
        GRADLE_OPTS = '-Dfile.encoding=UTF-8'
    }
    agent {
        dockerfile {
            args '--env BRANCH=${env.BRANCH} -v /data/capsulebuilds:/build/libs -v /data/capsulebuilds/cache:/root/.gradle'
        }
    }
    stages {
        stage('Build') {
            steps {
                sh "rm build/libs/*.jar || true"
                git branch: "${env.BRANCH}", url: 'https://github.com/Lythom/capsule.git'
                sh "sed -i 's/BUILD_ID/${env.BUILD_ID}/g' build.properties"
                sh '/gradlew build --stacktrace'
            }
        }
        stage('Test') {
            steps {
                echo 'TODO'
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: "build/libs/Capsule-${env.BRANCH}*.${env.BUILD_ID}.jar", fingerprint: true
                archiveArtifacts artifacts: "build/libs/Capsule-${env.BRANCH}*.${env.BUILD_ID}-sources.jar", fingerprint: true
            }
        }
    }
}
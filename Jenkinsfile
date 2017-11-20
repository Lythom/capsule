pipeline {
    agent {
        dockerfile {
            args '--env BRANCH=1.10 -v /data/capsulebuilds:/build/libs -v /data/capsulebuilds/cache:/root/.gradle'
        }
    }
    environment {
        GRADLE_OPTS = '-Dfile.encoding=UTF-8'
    }
    stages {
        stage('Build') {
            steps {
                git branch: '${env.BRANCH}', url: 'https://github.com/Lythom/capsule.git'
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
                archiveArtifacts '/build/libs'
            }
        }
    }
}
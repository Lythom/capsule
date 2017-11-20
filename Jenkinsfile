pipeline {
    agent {
        dockerfile {
            args '--env COMMIT=1.10 -v /data/capsulebuilds:/build/libs -v /data/capsulebuilds/cache:/root/.gradle'
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'build.sh'
            }
        }
    }
}
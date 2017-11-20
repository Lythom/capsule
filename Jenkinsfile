pipeline {
    agent {
        dockerfile true
        args '--env COMMIT=1.10 -v ~/capsulebuilds:/build/libs -v ~/capsulebuilds/cache:/root/.gradle'
    }
    stages {
        stage('Build') {
            steps {
                sh 'build.sh'
            }
        }
    }
}
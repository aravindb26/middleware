@Library('pipeline-library') _

import com.openexchange.jenkins.Trigger

String workspace // pwd()

// Branch name which represents the default behaviour of this pipeline. For main = main for stable-X.X = stable-X.X.
def COMMON_BRANCH = 'stable-8.19'

def skipRemainingStages = false

pipeline {
    agent none
    parameters {
        booleanParam(name: 'BLACKDUCK_SCANS', defaultValue: false, description: 'Execute Blackduck scans')
        booleanParam(name: 'COVERITY_SCANS', defaultValue: false, description: 'Execute Coverity scans')
        booleanParam(name: 'SKIP_DOC', defaultValue: false, description: 'Skip documentation build')
        booleanParam(name: 'CORE_TEST', defaultValue: false, description: 'Use core-test image (w/o office and documentconverter-api) instead of middleware-test image for testing')
        string(name: 'CORE_INTERNAL_BRANCH', defaultValue: COMMON_BRANCH, description: 'Sets the core-internal branch to a different value than main')
        string(name: 'GUARD_BRANCH', defaultValue: COMMON_BRANCH, description: 'Sets the guard branch to a different value than main')
        string(name: 'CORE_TEST_BRANCH', defaultValue: COMMON_BRANCH, description: 'Sets the core-test branch to a different value than main')
        string(name: 'INTEGRATION_BRANCH', defaultValue: COMMON_BRANCH, description: 'Sets the appsuite integration branch to a different value than main')
    }
    environment {
        GIT_MIRROR_HOME = '/shared-data/jenkins/appsuite'
    }
    options {
        buildDiscarder(logRotator(daysToKeepStr: '30'))
        skipDefaultCheckout()
        disableConcurrentBuilds()
    }
    triggers {
        cron('main' == env.BRANCH_NAME ? 'H H(20-23) * * 1-5' : '')
    }
    stages {
        stage('Blackduck') {
            agent {
                kubernetes {
                    yamlFile 'blackduckPod.yaml'
                }
            }
            when {
                beforeAgent true
                allOf {
                    branch 'main'
                    anyOf {
                        triggeredBy('TimerTrigger')
                        expression { params.BLACKDUCK_SCANS == true}
                    }
                }
            }
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${getBranchName()}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'backend'], [$class: 'CloneOption', reference: "$GIT_MIRROR_HOME/backend.git"]],
                    gitTool: 'Linux',
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: "9a40d6b1-813a-4c46-9b0d-18320a0a4ef4", url: 'git@gitlab.open-xchange.com:middleware/core.git']]
                ])
                container('ant') {
                    dir('backend/com.openexchange.bundles') {
                        sh 'ant -f java-commons.xml'
                    }
                }
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    container('blackduck') {
                        scanBlackDuckHub("backend", "Product: OX App Suite Middleware", "main", "7.3.3")
                    }
                }
            }
        }
        stage('Coverity') {
            agent {
                kubernetes {
                    yamlFile 'coverityPod.yaml'
                }
            }
            when {
                beforeAgent true
                allOf {
                    branch 'main'
                    anyOf {
                        triggeredBy('TimerTrigger')
                        expression{ params.COVERITY_SCANS == true }
                    }
                }
            }
            environment {
                coverityIntermediateDir = 'analyze-idir'
                coverityBuildDir = 'build'
            }
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${getBranchName()}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'backend'], [$class: 'CloneOption', reference: "$GIT_MIRROR_HOME/backend.git"]],
                    gitTool: 'Linux',
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: "9a40d6b1-813a-4c46-9b0d-18320a0a4ef4", url: 'git@gitlab.open-xchange.com:middleware/core.git']]
                ])
                container('ant') {
                    dir('backend/com.openexchange.bundles') {
                        sh 'ant -f java-commons.xml'
                    }
                }
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    container('coverity') {
                        dir('backend') {
                            scanCoverity('middleware-core', "git@gitlab.open-xchange.com:middleware/core.git", 'main', "./gradlew clean build -x test -Pcoverity --no-daemon", "")
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts(allowEmptyArchive: true, artifacts: "${coverityIntermediateDir}/build-log.txt")
                    archiveArtifacts(allowEmptyArchive: true, artifacts: "${coverityBuildDir}/**")
                    archiveArtifacts(allowEmptyArchive: true, artifacts: "${coverityIntermediateDir}/output/analysis-log.txt")
                    archiveArtifacts(allowEmptyArchive: true, artifacts: "${coverityIntermediateDir}/output/distributor.log")
                }
            }
        }
        stage('POT') {
            agent {
                kubernetes {
                    yamlFile 'jenkinsPod.yaml'
                }
            }
            environment {
                GITLAB = credentials('e9b6b0ea-2d95-4d57-ac38-f64189c5be27')
            }
            when {
                beforeAgent true
                allOf {
                    expression { version4Documentation(env.BRANCH_NAME) == null }
                    expression { skipRemainingStages == false }
                }
            }
            stages {
                stage('Checkout') {
                    steps {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${getBranchName()}"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'backend'], [$class: 'CloneOption', reference: "$GIT_MIRROR_HOME/backend.git"]],
                            gitTool: 'Linux',
                            submoduleCfg: [],
                            userRemoteConfigs: [[url: "https://gitlab.open-xchange.com/middleware/core.git"]]
                        ])
                    }
                }
                stage('POT Commit Check') {
                    steps {
                        dir('backend') {
                            script {
                                def lastCommitMessage = sh(returnStdout: true, script: 'git show -s --format=%s').trim()
                                def previousBuild = currentBuild.getPreviousBuild()
                                if (previousBuild != null) {
                                    if (lastCommitMessage.equals("Automatic POT generation") && previousBuild.result.equals('SUCCESS')) {
                                        skipRemainingStages = true
                                    }
                                }
                            }
                        }
                    }
                }
                stage('POT generation') {
                    when {
                        expression { skipRemainingStages == false }
                    }
                    steps {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: '*/master']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'automation']],
                            gitTool: 'Linux',
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: '9a40d6b1-813a-4c46-9b0d-18320a0a4ef4', url: "git@gitlab.open-xchange.com:engineering/automation.git"]]
                        ])
                        dir('automation/backendI18N') {
                            container('toox-proxy') {
                                sh "ant -file backendPot.xml -DcheckoutDir=${env.WORKSPACE}/backend -DproductToGenerate=backend -DpotDir=${env.WORKSPACE}/backend/l10n create-server-pot"
                            }
                        }
                        dir('backend') {
                            script {
                                def gitStatus = sh script: 'git status --porcelain', returnStdout: true
                                if (gitStatus.contains('l10n/backend.pot')) {
                                    sh 'git config user.email scriptuser@open-xchange.com'
                                    sh 'git config user.name Jenkins'
                                    sh 'git add l10n/backend.pot'
                                    sh 'git commit -m "Automatic POT generation"'
                                    sh 'git show HEAD'
                                    sh "git remote set-url origin https://$GITLAB_USR:$GITLAB_PSW@gitlab.open-xchange.com/middleware/core.git"
                                    sh "git push origin HEAD:${getBranchName()}"
                                }
                            }
                        }
                    }
                    post {
                        success {
                            archiveArtifacts artifacts: 'backend/l10n/backend.pot', onlyIfSuccessful: true
                        }
                        always {
                            dir('automation') {
                                deleteDir()
                            }
                        }
                    }
                }
            }
        }
        stage('Integration build') {
            agent {
                kubernetes {
                  yamlFile 'integrationBuildPod.yaml'
                }
            }
            when {
                expression { skipRemainingStages == false }
            }
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${getBranchName()}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'backend'], [$class: 'CloneOption', reference: "$GIT_MIRROR_HOME/backend.git"]],
                    gitTool: 'Linux',
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: "9a40d6b1-813a-4c46-9b0d-18320a0a4ef4", url: 'git@gitlab.open-xchange.com:middleware/core.git']]
                ])
                dir('backend') {
                    script {
                        env.GIT_COMMIT = sh(returnStdout: true, script: "git rev-parse HEAD").trim()
                    }
                }
                script {
                    def buildJob
                    if ("${getBranchName()}" == COMMON_BRANCH) {
                        buildJob = build job: "appsuite/integration/${params.INTEGRATION_BRANCH}", wait: true,
                        parameters: [
                            booleanParam(name: 'CORE_TEST', value: params.CORE_TEST),
                            string(name: 'CORE_TEST_BRANCH', value: params.CORE_TEST_BRANCH)
                        ]
                    } else {
                        buildJob = build job: "appsuite/integration/${params.INTEGRATION_BRANCH}", wait: true,
                        parameters: [
                            text(name: 'OVERWRITE_COMPONENTS', value: "[{\"name\":\"backend\",\"branch\":\"${getBranchName()}\",\"commit\":\"${env.GIT_COMMIT}\"}," +
                            "{\"name\":\"core-internal\",\"branch\":\"${params.CORE_INTERNAL_BRANCH}\"}," +
                            "{\"name\":\"guard\",\"branch\":\"${params.GUARD_BRANCH}\"}]"),
                            booleanParam(name: 'CORE_TEST', value: params.CORE_TEST),
                            string(name: 'CORE_TEST_BRANCH', value: params.CORE_TEST_BRANCH)
                        ]
                    }
                    env.VERSION_NUMBER = buildJob.getBuildVariables()["VERSION_NUMBER"]
                    if (env.VERSION_NUMBER == null || env.VERSION_NUMBER == '' )  {
                         currentBuild.result = 'ABORTED'
                         error('AppVersion is empty, check integration build!')
                    }
                }
            }
        }
        stage('Documentation generation') {
            agent {
                kubernetes {
                    yamlFile 'jenkinsPod.yaml'
                }
            }
            when {
                beforeAgent true
                allOf {
                    expression { version4Documentation(env.BRANCH_NAME) != null }
                    expression { params.CORE_TEST_BRANCH == COMMON_BRANCH && params.INTEGRATION_BRANCH == COMMON_BRANCH }
                    expression { skipRemainingStages == false }
                }
            }
            stages {
                stage('Checkout') {
                    steps {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${getBranchName()}"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'backend'], [$class: 'CloneOption', reference: "$GIT_MIRROR_HOME/backend.git"]],
                            gitTool: 'Linux',
                            submoduleCfg: [],
                            userRemoteConfigs: [[url: 'https://gitlab.open-xchange.com/middleware/core.git']]
                        ])
                    }
                }
                stage('Configuration documentation') {
                    when {
                        allOf {
                            expression { version4Documentation(env.BRANCH_NAME) != null }
                            expression { params.SKIP_DOC == false }
                        }
                    }
                    steps {
                        script {
                            def targetVersion = version4Documentation(env.BRANCH_NAME)
                            def targetDirectory
                            dir('config-doc-processor') {
                                // Need to do some file operation in directory otherwise it is not created.
                                writeFile file: 'properties.json', text: ''
                                targetDirectory = pwd()
                            }
                            container('ant') {
                                dir('backend/com.openexchange.bundles') {
                                    sh 'ant -f java-commons.xml'
                                }
                            }
                            container('gradle') {
                                dir('backend/documentation-generic/config') {
                                    sh "gradle --no-daemon runConfigDocuProcessor -PtargetDirectory=${targetDirectory} -PtargetVersion=${targetVersion}"
                                }
                            }
                            dir('config-doc-processor') {
                                sshPublisher(
                                    publishers: [
                                        sshPublisherDesc(
                                            configName: 'documentation',
                                            transfers: [
                                                sshTransfer(
                                                    cleanRemote: false,
                                                    excludes: '',
                                                    execCommand: '',
                                                    execTimeout: 120000,
                                                    flatten: false,
                                                    makeEmptyDirs: false,
                                                    noDefaultExcludes: false,
                                                    patternSeparator: '[, ]+',
                                                    remoteDirectory: "components/middleware/config/${targetVersion}",
                                                    remoteDirectorySDF: false,
                                                    removePrefix: '',
                                                    sourceFiles: 'properties.json'
                                                )
                                            ],
                                            usePromotionTimestamp: false,
                                            useWorkspaceInPromotion: false,
                                            verbose: true
                                        )
                                    ]
                                )
                            }
                            build job: 'middleware/propertyDocumentationUI/master', parameters: [string(name: 'targetVersion', value: targetVersion), string(name: 'targetDirectory', value: 'middleware/config')]
                        }
                    }
                    post {
                        success {
                            archiveArtifacts 'config-doc-processor/properties.json'
                        }
                    }
                }
                stage('HTTP API documentation') {
                    when {
                        allOf {
                            expression { version4Documentation(env.BRANCH_NAME) != null }
                            expression { params.SKIP_DOC == false }
                        }
                    }
                    steps {
                        script {
                            def targetVersion = version4Documentation(env.BRANCH_NAME)
                            container('gradle') {
                                dir('backend') {
                                    sh 'gradle --no-daemon :http-api:generateHttpApiDoc :drive-api:generateHttpApiDoc :rest-api:generateHttpApiDoc'
                                }
                            }
                            dir('backend/documentation-generic/') {
                                sshPublisher(
                                    publishers: [
                                        sshPublisherDesc(
                                            configName: 'documentation',
                                            transfers: [
                                                    sshTransfer(
                                                        cleanRemote: true,
                                                        excludes: '',
                                                        execCommand: '',
                                                        execTimeout: 120000,
                                                        flatten: false,
                                                        makeEmptyDirs: false,
                                                        noDefaultExcludes: false,
                                                        patternSeparator: '[, ]+',
                                                        remoteDirectory: "components/middleware/http/${targetVersion}",
                                                        remoteDirectorySDF: false,
                                                        removePrefix: 'http_api/documents/html',
                                                        sourceFiles: 'http_api/documents/html/**'),
                                                    sshTransfer(
                                                        cleanRemote: true,
                                                        excludes: '',
                                                        execCommand: '',
                                                        execTimeout: 120000,
                                                        flatten: false,
                                                        makeEmptyDirs: false,
                                                        noDefaultExcludes: false,
                                                        patternSeparator: '[, ]+',
                                                        remoteDirectory: "components/middleware/drive/${targetVersion}",
                                                        remoteDirectorySDF: false,
                                                        removePrefix: 'drive_api/documents/html',
                                                        sourceFiles: 'drive_api/documents/html/**'),
                                                    sshTransfer(
                                                        cleanRemote: true,
                                                        excludes: '',
                                                        execCommand: '',
                                                        execTimeout: 120000,
                                                        flatten: false,
                                                        makeEmptyDirs: false,
                                                        noDefaultExcludes: false,
                                                        patternSeparator: '[, ]+',
                                                        remoteDirectory: "components/middleware/rest/${targetVersion}",
                                                        remoteDirectorySDF: false,
                                                        removePrefix: 'rest_api/documents/html',
                                                        sourceFiles: 'rest_api/documents/html/**')
                                            ],
                                            usePromotionTimestamp: false,
                                            useWorkspaceInPromotion: false,
                                            verbose: true
                                        )
                                    ]
                                )
                            }
                        }
                    }
                }
                stage('Markdown Documentation') {
                    when {
                        expression { version4Documentation(env.BRANCH_NAME) != null }
                    }
                    steps {
                        script {
                            def targetVersion = version4Documentation(env.BRANCH_NAME)
                            def branchName = env.BRANCH_NAME
                            container('jekyll'){
                                sh """
                                    tail -n +3 ${WORKSPACE}/backend/CHANGELOG.md >> ${WORKSPACE}/backend/documentation/index.md
                                    sed -i 's+https://jira.open-xchange.com/browse/SCR+https://documentation.open-xchange.com/${targetVersion}/middleware/detailed_software_changes.html#scr+gI' ${WORKSPACE}/backend/documentation/index.md
                                    ln -s ${WORKSPACE}/backend/documentation /documentation/jekyll/_middleware
                                    cd /documentation
                                    echo branchName: "/${branchName}" >> _config.yml
                                    cat _config.yml
                                    bundle exec jekyll b --baseurl /${targetVersion} --config _config.yml
                                    cd ${WORKSPACE}
                                    mkdir -p doku/${targetVersion}
                                    cp -r /documentation/dist/* doku/${targetVersion}
                                """
                            }
                            dir('doku') {
                                sshPublisher(
                                    publishers: [
                                        sshPublisherDesc(
                                            configName: 'documentation',
                                            transfers: [
                                                sshTransfer(
                                                    cleanRemote: false,
                                                    excludes: '',
                                                    execCommand: '',
                                                    execTimeout: 120000,
                                                    flatten: false,
                                                    makeEmptyDirs: false,
                                                    noDefaultExcludes: false,
                                                    patternSeparator: '[, ]+',
                                                    remoteDirectorySDF: false,
                                                    sourceFiles: "${targetVersion}/**"
                                                )
                                            ],
                                            usePromotionTimestamp: false,
                                            useWorkspaceInPromotion: false,
                                            verbose: true
                                        )
                                    ]
                                )
                            }
                        }
                    }
                }
            }
        }
        stage('Helm') {
            agent {
                kubernetes {
                    yamlFile 'helmPod.yaml'
                }
            }
            when {
                beforeAgent true
                allOf {
                    expression { version4Documentation(env.BRANCH_NAME) != null }
                    expression { params.CORE_TEST_BRANCH == COMMON_BRANCH && params.INTEGRATION_BRANCH == COMMON_BRANCH }
                    expression { skipRemainingStages == false }
                }
            }
            environment {
                oxComponent = 'core-mw'
                CI_REGISTRY = credentials('0fa3bb8a-cc55-4752-8d4d-636568bd8b23')
            }
            stages {
                stage('Checkout') {
                    steps {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${getBranchName()}"]],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'backend'], [$class: 'CloneOption', reference: "$GIT_MIRROR_HOME/backend.git"]],
                            gitTool: 'Linux',
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: "9a40d6b1-813a-4c46-9b0d-18320a0a4ef4", url: 'git@gitlab.open-xchange.com:middleware/core.git']]
                        ])
                    }
                }
                stage('Linting Helm Chart') {
                    steps{
                        container('k8s') {
                            dir('backend') {
                                script{
                                    sh 'helm registry login registry.open-xchange.com -u $CI_REGISTRY_USR -p $CI_REGISTRY_PSW'
                                    sh "helm dep up helm/${oxComponent}"
                                    sh "helm lint helm/${oxComponent} --set mysql.auth.user=${env.BUILD_NUMBER} --set mysql.auth.password=${env.BUILD_NUMBER} --set mysql.auth.rootPassword=${env.BUILD_NUMBER}"
                                }
                            }
                        }
                    }
                }
                stage('Publishing Helm Chart') {
                    steps {
                        container('k8s') {
                            dir('backend') {
                                script {
                                    def chart = readYaml file: "helm/${oxComponent}/Chart.yaml"
                                    def registryUrl = 'https://registry.open-xchange.com'
                                    def apiUrl = "${registryUrl}/api/v2.0/projects/appsuite-core-internal/repositories/charts%252Fcore-mw/artifacts/${chart.version}/tags"

                                    def response = sh(script: "curl -s -w %{http_code} -o /dev/null -u ${env.CI_REGISTRY_USR}:${env.CI_REGISTRY_PSW} ${apiUrl}", returnStdout: true).trim()

                                    if (response == '404') {
                                        echo "Publishing Helm Chart (${chart.version})"
                                        sh "helm dep up helm/${oxComponent}"
                                        sh "helm package helm/${oxComponent} --app-version ${env.VERSION_NUMBER}"
                                        sh "helm push ${oxComponent}-${chart.version}.tgz oci://${registryUrl}/appsuite-core-internal/charts"
                                    } else {
                                        echo "Skip publishing of the helm chart because ${chart.version} already exists."
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        failure {
            emailext attachLog: true,
                body: "${env.BUILD_URL} failed.\n\nFull log at: ${env.BUILD_URL}console\n\n",
                subject: "${env.JOB_NAME} (#${env.BUILD_NUMBER}) - ${currentBuild.result}",
                recipientProviders: [[$class: 'RequesterRecipientProvider'], [$class: 'UpstreamComitterRecipientProvider'], [$class: 'DevelopersRecipientProvider']],
                to: 'backend@open-xchange.com'
        }
    }
}

String version4Documentation(String branchName) {
    if ('main' == branchName)
        return 'main'
    if (branchName.startsWith('stable-'))
        return branchName.substring(7)
    return null
}

String getBranchName() {
    def branchName = env.BRANCH_NAME
    if (branchName.startsWith("MR-")) {
        branchName = env.CHANGE_BRANCH
    }
    return branchName
}
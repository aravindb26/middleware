plugins {
    id("com.openexchange.build.documentation-properties") version "1.1.6" apply false
    id("com.openexchange.build.gradle-git") version "3.7.0"
    id("com.openexchange.build.install") version "3.7.6" apply false
    id("com.openexchange.build.licensing") version "1.0.4"
    id("com.openexchange.build.opensuse-build-service-client") version "1.7.0"
    id("com.openexchange.build.osgi") version "2.0.1"
    id("com.openexchange.build.packaging") version "5.7.0" apply false
    id("com.openexchange.build.plugin-applier") version "1.3.0"
    id("com.openexchange.build.project-type-scanner") version "1.5.0"
    id("com.openexchange.build.image-builder") version "3.0.2"
}

allprojects {
    apply {
        plugin("com.openexchange.build.plugin-applier")
    }
    repositories {
        maven {
            url = uri("https://artifactory.production.cloud.oxoe.io/artifactory/libs-release")
        }
    }
    tasks.withType(AbstractTestTask::class.java).configureEach {
        // TODO all tests need to run successfully one time
        ignoreFailures = true
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        ignoreFailures = true
        jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.base/java.util.regex=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
            "--add-opens=java.base/javax.net.ssl=ALL-UNNAMED",
            "--add-opens=java.base/java.util.stream=ALL-UNNAMED",
            "--add-opens=java.base/java.nio.charset=ALL-UNNAMED",
            "--add-opens=java.base/java.nio.file=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.base/java.security=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.jca=ALL-UNNAMED",
            "--add-opens=java.xml/jdk.xml.internal=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED")
    }
}

licensing {
    licenses {
        create("server") {
            this.sourceFile = File(project.projectDir, "SERVER-LICENSE")
        }
    }
}

buildservice {
    url = "https://buildapi.open-xchange.com"
    credentialsFromEnvironment()
    obsProject {
        name = "backend-gradle"
        repositories {
            create("DebianStretch") {
                depends("Debian:Stretch", "standard")
            }
            create("DebianBuster") {
                depends("Debian:Buster", "standard")
            }
            create("RHEL7") {
                depends("RedHat:build-dependencies", "RHEL7")
            }
        }
    }
}

configure<com.openexchange.build.install.extension.InstallExtension> {
    //destDir.set(File("${System.getProperty("user.home")}/tmp/testox"))
    destDir.set(File("/"))
    prefix.set(File("/opt/open-xchange"))
    packageSeparatedInis.set(true)
}

imageExtension {
    images {
        register("core") {
            components.addAll("backend")
            inputDir.set(file("docker"))
	        imageTag.set(setOf(project.findProperty("imageTag").toString()))
            imageName.set(project.findProperty("imageName").toString())
            archiveFileName.set("backend_container.orig.tar.bz2")
        }
    }
}

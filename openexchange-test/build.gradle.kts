plugins {
    java
}

dependencies {
    rootProject.childProjects["com.openexchange.bundles"]?.files(
        "jars/commons-codec-1.15.jar",
        "jars/commons-io-2.11.0.jar",
        "jars/commons-validator-1.6.jar",
        "jars/httpclient-osgi-4.5.14.jar",
        "jars/httpcore-osgi-4.4.16.jar",
        "jars/jackson-annotations-2.13.3.jar",
        "jars/jackson-core-2.13.3.jar",
        "jars/jackson-databind-2.13.3.jar",
        "jars/javax.servlet-api-4.0.0.jar",
        "jars/joda-time-2.10.5.jar",
        "jars/org.apache.commons.lang_2.6.0.v201404270220.jar",
        "jars/org.eclipse.osgi_3.16.300.v20210525-1715.jar",
        "jars/org.eclipse.osgi.services_3.10.100.v20210324-0936.jar",
        "jars/pdfbox-2.0.24.jar",
        "jars/slf4j-api-1.7.32.jar",
        "jars/gson-2.10.1.jar"
    )?.let { testImplementation(it) }
    rootProject.childProjects["com.openexchange.socketio"]?.files(
        "lib/engine.io-client-1.0.0.jar",
        "lib/engine.io-server-1.3.4.jar",
        "lib/socket.io-client-1.0.0.jar",
        "lib/socket.io-server-1.0.0.jar"
    )?.let { testImplementation(it) }
    rootProject.childProjects["com.openexchange.soap.common"]?.files(
        "lib/cxf-core-3.5.5.jar",
        "lib/cxf-rt-frontend-simple-3.5.5.jar",
        "lib/jaxws-api-2.3.1.jar"
    )?.let { testImplementation(it) }
    rootProject.childProjects["com.openexchange.mail.export.pdf"]?.files(
            "lib/pdfbox-2.0.27.jar",
            "lib/pdfbox-tools-2.0.27.jar"
    )?.let { testImplementation(it) }
    testImplementation(project(":com.google.api.client"))
    testImplementation(project(":com.google.guava"))
    testImplementation(project(":com.openexchange.admin.plugin.hosting"))
    testImplementation(project(":com.openexchange.admin.rmi"))
    testImplementation(project(":com.openexchange.advertisement"))
    testImplementation(project(":com.openexchange.advertisement.json"))
    testImplementation(project(":com.openexchange.caching"))
    testImplementation(project(":com.openexchange.caching.events"))
    testImplementation(project(":com.openexchange.calendar.json"))
    testImplementation(project(":com.openexchange.capabilities"))
    testImplementation(project(":com.openexchange.capabilities.impl"))
    testImplementation(project(":com.openexchange.charset"))
    testImplementation(project(":com.openexchange.chronos"))
    testImplementation(project(":com.openexchange.chronos.common"))
    testImplementation(project(":com.openexchange.chronos.ical"))
    testImplementation(project(":com.openexchange.chronos.ical.ical4j"))
    testImplementation(project(":com.openexchange.chronos.json"))
    testImplementation(project(":com.openexchange.chronos.scheduling.common"))
    testImplementation(project(":com.openexchange.client.onboarding"))
    testImplementation(project(":com.openexchange.cluster.timer"))
    testImplementation(project(":com.openexchange.cluster.timer.impl"))
    testImplementation(project(":com.openexchange.common"))
    testImplementation(project(":com.openexchange.config.cascade.impl"))
    testImplementation(project(":com.openexchange.config.cascade.impl", "testClasses"))
    testImplementation(project(":com.openexchange.config.lean"))
    testImplementation(project(":com.openexchange.contact.provider"))
    testImplementation(project(":com.openexchange.configread", "testClasses"))
    testImplementation(project(":com.openexchange.contact.storage.rdb"))
    testImplementation(project(":com.openexchange.contacts.json"))
    testImplementation(project(":com.openexchange.conversion"))
    testImplementation(project(":com.openexchange.conversion.engine"))
    testImplementation(project(":com.openexchange.data.conversion.ical.ical4j"))
    testImplementation(project(":com.openexchange.database"))
    testImplementation(project(":com.openexchange.database.migration"))
    testImplementation(project(":com.openexchange.datatypes.genericonf"))
    testImplementation(project(":com.openexchange.drive"))
    testImplementation(project(":com.openexchange.drive.impl"))
    testImplementation(project(":com.openexchange.external.account"))
    testImplementation(project(":com.openexchange.file.storage"))
    testImplementation(project(":com.openexchange.file.storage.composition"))
    testImplementation(project(":com.openexchange.file.storage.json"))
    testImplementation(project(":com.openexchange.file.storage.json", "testClasses"))
    testImplementation(project(":com.openexchange.filestore"))
    testImplementation(project(":com.openexchange.filestore.impl"))
    testImplementation(project(":com.openexchange.folder.json"))
    testImplementation(project(":com.openexchange.gdpr.dataexport"))
    testImplementation(project(":com.openexchange.global"))
    testImplementation(project(":com.openexchange.html", "testClasses"))
    testImplementation(project(":com.openexchange.http.grizzly"))
    testImplementation(project(":com.openexchange.i18n"))
    testImplementation(project(":com.openexchange.imagetransformation"))
    testImplementation(project(":com.openexchange.imagetransformation.java"))
    testImplementation(project(":com.openexchange.imap"))
    testImplementation(project(":com.openexchange.importexport"))
    testImplementation(project(":com.openexchange.java"))
    testImplementation(project(":com.openexchange.jcharset"))
    testImplementation(project(":com.openexchange.jslob"))
    testImplementation(project(":com.openexchange.logging"))
    testImplementation(project(":com.openexchange.mail.autoconfig"))
    testImplementation(project(":com.openexchange.mail.autoconfig.impl"))
    testImplementation(project(":com.openexchange.mail.categories"))
    testImplementation(project(":com.openexchange.messaging"))
    testImplementation(project(":com.openexchange.messaging.generic"))
    testImplementation(project(":com.openexchange.metrics"))
    testImplementation(project(":com.openexchange.metrics.dropwizard"))
    testImplementation(project(":com.openexchange.monitoring"))
    testImplementation(project(":com.openexchange.multifactor"))
    testImplementation(project(":com.openexchange.net.ssl"))
    testImplementation(project(":com.openexchange.net.ssl.config"))
    testImplementation(project(":com.openexchange.net.ssl.config.impl"))
    testImplementation(project(":com.openexchange.oauth.provider"))
    testImplementation(project(":com.openexchange.oauth.provider.impl"))
    testImplementation(project(":com.openexchange.oauth.provider.rmi"))
    testImplementation(project(":com.openexchange.osgi"))
    testImplementation(project(":com.openexchange.password.mechanism"))
    testImplementation(project(":com.openexchange.push"))
    testImplementation(project(":com.openexchange.push.udp"))
    testImplementation(project(":com.openexchange.quota"))
    testImplementation(project(":com.openexchange.request.analyzer.rest"))
    testImplementation(project(":com.openexchange.server", "testClasses"))
    testImplementation(project(":com.openexchange.sessiond"))
    testImplementation(project(":com.openexchange.sessiond.rmi"))
    testImplementation(project(":com.openexchange.sessionstorage"))
    testImplementation(project(":com.openexchange.share"))
    testImplementation(project(":com.openexchange.share.impl"))
    testImplementation(project(":com.openexchange.sms"))
    testImplementation(project(":com.openexchange.sms.sipgate"))
    testImplementation(project(":com.openexchange.sms.tools"))
    testImplementation(project(":com.openexchange.spamhandler.default"))
    testImplementation(project(":com.openexchange.admin.soap.resource"))
    testImplementation(project(":com.openexchange.admin.soap.context"))
    testImplementation(project(":com.openexchange.admin.soap.group"))
    testImplementation(project(":com.openexchange.admin.soap.user"))
    testImplementation(project(":com.openexchange.admin.soap.util"))
    testImplementation(project(":com.openexchange.oauth.provider.soap"))
    testImplementation(project(":com.openexchange.subscribe", "testClasses"))
    testImplementation(project(":com.openexchange.subscribe.json"))
    testImplementation(project(":com.openexchange.tasks.json"))
    testImplementation(project(":com.openexchange.threadpool"))
    testImplementation(project(":com.openexchange.subscribe"))
    testImplementation(project(":com.openexchange.test", "testClasses"))
    testImplementation(project(":com.openexchange.test", "testLibraries"))
    testImplementation(project(":com.openexchange.tokenlogin"))
    testImplementation(project(":com.openexchange.tx"))
    testImplementation(project(":com.openexchange.user.json"))
    testImplementation(project(":com.openexchange.userfeedback.rest"))
    testImplementation(project(":com.openexchange.version"))
    testImplementation(project(":com.openexchange.xing"))
    testImplementation(project(":com.openexchange.xml"))
    testImplementation(project(":javax.mail"))
    testImplementation(project(":net.fortuna.ical4j"))
    testImplementation(project(":org.json"))
    testImplementation(project(":com.squareup.okhttp3"))
    testImplementation(project(":com.openexchange.mail.exportpdf.impl"))


    // TODO try to eliminate these jar file dependencies
    testImplementation(files("lib/cardme-0.4.0.jar"))
    testImplementation(files("lib/commons-httpclient-3.1.jar"))
    testImplementation(files("lib/httpunit-1.7.jar"))
    testImplementation(files("lib/jackrabbit-webdav-2.3.3.jar"))
    testImplementation(files("lib/jackson-datatype-joda-2.11.0.jar"))
    // jersey is used by generated API, too
    testImplementation(files("lib/jersey-client-2.32.jar"))
    testImplementation(files("lib/jersey-common-2.32.jar"))
    testImplementation(files("lib/jersey-media-multipart-2.32.jar"))
    testImplementation(files("lib/jersey-server-2.32.jar"))
    testImplementation(files("lib/jersey-test-framework-core-2.32.jar"))
    testImplementation(files("lib/jersey-guava-2.25.1.jar"))
    testImplementation(files("lib/js.jar"))
    testImplementation(files("lib/jvyaml-0.2.jar"))
    testImplementation(files("lib/tempus-fugit-1.2-20140129.191141-5.jar"))
    testImplementation(files("lib/xmlwise-1_2.jar"))
    testImplementation(files("lib/okhttp-urlconnection-4.11.0.jar"))
    testImplementation(files("lib/kotlin-stdlib-1.9.10.jar"))
    testImplementation(files("lib/jboss-jms-api.jar"))

    testImplementation(files("lib/jersey-test-framework-provider-grizzly2-2.32.jar"))
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:2.32")

    //testImplementation(project(":drive-api"))
    testImplementation(project(":http-api"))
    testImplementation(project(":rest-api"))

    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.platform:junit-platform-suite-engine")
}

tasks {
    val writeConfig = register<WriteProperties>("writeConfig") {
        onlyIf {
            file("conf/ajax-custom.properties").exists() == false
        }
        val properties = listOf("serverhostname", "protocol", "rmihost", "davhost", "mockhostname", "mockport", "singlenodehostname", "mailHost", "mailTransportHost", "serverPort", "basePath", "createContextAndUser")
        outputFile = file("conf/ajax-custom.properties").also { it.parentFile.mkdirs() }
        properties.forEach { property ->
            project.findProperty(property)?.let { property(property, it) }
                ?: logger.warn("$property is NULL using default value for ajax.properties!")
        }
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            sources {
                java {
                    setSrcDirs(files())
                    srcDir(projectDir.resolve("src"))
                }
            }
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                        val maxForks: Int = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
                        val maxForksOverride : String = System.getenv("MAX_PARALLEL_FORKS") ?: ""
                        maxParallelForks = if(maxForksOverride.isNotEmpty()) Integer.parseInt(maxForksOverride) else maxForks
                        logger.lifecycle("Using {} test processes in parallel", maxParallelForks)

                        maxHeapSize = "256M"
                        systemProperties(
                            mapOf(
                                "logback.configurationFile" to "conf/logback.xml",
                                "junit.jupiter.execution.parallel.enabled" to true,
                                "junit.jupiter.execution.parallel.mode.default" to "concurrent",
                                "junit.jupiter.execution.parallel.mode.classes.default" to "concurrent",
                                "junit.jupiter.execution.parallel.config.strategy" to "dynamic"
                            )
                        )
                    }
                }
            }
        }
    }
}
val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}

val testJar by tasks.registering(Jar::class) {
    archiveClassifier.set("tests")
    from(project.the<SourceSetContainer>()["integrationTest"].output)
}

val testClasses by configurations.creating {
    extendsFrom(configurations.testRuntimeClasspath.get())
    description = "API elements for test."
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, Usage.JAVA_RUNTIME))
    }
    outgoing {
        // default group:name:version
        capability("${project.group}:${project.name}-tests:${project.version}")
    }
}

artifacts {
    add(testClasses.name, testJar)
}

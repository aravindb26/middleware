plugins {
    `java-library`
}

dependencies {
    api(fileTree("testlib").matching {
        include("*.jar")
    })
}

val testClasses by configurations.creating {
    extendsFrom(configurations["testRuntimeClasspath"])
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

val testLibraries by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, Usage.JAVA_RUNTIME))
    }
    outgoing {
        capability("$group:${project.name}-testlibs:$version")
    }
}

val testJar by tasks.registering(Jar::class) {
    archiveClassifier.set("tests")
    from(project.the<SourceSetContainer>()["test"].output)
}

artifacts {
    add(testClasses.name, testJar)
    project.files(
        "testlib/hamcrest-date-2.0.7.jar",
        "testlib/hamcrest-2.2.jar",
        "testlib/mockito-core-3.11.2.jar",
        "testlib/powermock-reflect-2.0.9.jar"
    ).forEach {
        add(testLibraries.name, it)
    }
}

import org.gradle.plugins.ide.eclipse.model.internal.FileReferenceFactory
import org.gradle.plugins.ide.eclipse.model.FileReference

plugins {
    `java-library`
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

val testJar by tasks.registering(Jar::class) {
    archiveClassifier.set("tests")
    from(project.the<SourceSetContainer>()["test"].output)
}

artifacts {
    add(testClasses.name, testJar)
}
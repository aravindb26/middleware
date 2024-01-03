plugins {
    java
}

dependencies {
    implementation("com.openexchange.appsuite.mw", "config-doc-processor", "1.+")
}

tasks.register<JavaExec>("runConfigDocuProcessor") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.openexchange.config.docu.parser.Parser")
    val arguments = mutableListOf<String>()
    arguments.add(project.projectDir.absolutePath)
    if (project.hasProperty("targetDirectory")) {
        arguments.add(project.property("targetDirectory").toString())
    }
    if (project.hasProperty("targetVersion")) {
        arguments.add(project.property("targetVersion").toString())
    }
    args(arguments)
}

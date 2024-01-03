import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

repositories {
    maven {
        url = uri("https://artifactory.production.cloud.oxoe.io/artifactory/libs-release")
    }
}

plugins {
    java
    id("org.openapi.generator") version "5.4.0"
    id("com.openexchange.gradle.plugin.openapi-resolver") version "1.0.2"
}

val documentationTools by configurations.creating
val openAPIJSONFile = buildDir.resolve("openAPI.json")

openApiValidate {
    inputSpec.set(openAPIJSONFile.absolutePath)
}

openApiGenerate {
    generatorName.set("java")
    generateAliasAsModel.set(true)
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configFile.set(projectDir.parentFile.resolve("client-gen/config/http_api.json").absolutePath)
    templateDir.set(projectDir.parentFile.resolve("client-gen/templates/").absolutePath)
    inputSpec.set(openAPIJSONFile.absolutePath)
    outputDir.set(buildDir.resolve("openapi-java-client").toString())
}

dependencies {
    implementation("io.swagger:swagger-annotations:1.5.24")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("io.gsonfire:gson-fire:1.8.4")
    implementation("org.openapitools:jackson-databind-nullable:0.2.1")
    implementation("org.apache.oltu.oauth2:org.apache.oltu.oauth2.client:1.0.1")
    implementation("org.apache.commons:commons-lang3:3.10")
    implementation("jakarta.annotation:jakarta.annotation-api:1.3.5")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-core:3.11.2")
    documentationTools("com.openexchange.appsuite.mw", "documentation-tools", "1.0-SNAPSHOT") {
        repositories {
            maven {
                url = uri("https://artifactory.production.cloud.oxoe.io/artifactory/libs-snapshot-local")
            }
        }
    }
}

resolve {
    rootFile.set(project.file("index.yaml"))
    outputFile.set(file(buildDir.resolve("openAPI.json")))
}

val resolve = tasks.named("resolve", com.openexchange.gradle.plugin.resolver.ResolveTask::class.java)
val openApiGenerate = tasks.named("openApiGenerate", GenerateTask::class.java) {
    dependsOn(resolve)
}

val generateHttpApiDoc = tasks.register("generateHttpApiDoc") {
    dependsOn("insertMarkdown")
}

//TODO Refactor to Gradle plugin
tasks.register("buildHtml", JavaExec::class.java) {
    dependsOn(resolve)
    workingDir("../")
    val fromFolder = project.projectDir.name + "/build"
    val toFolder = "../documentation-generic/${project.projectDir.name}"
    val fileName = "openAPI.json"

    val arguments = mutableListOf(
        fromFolder,
        toFolder,
        fileName
    )

    classpath = configurations.getByName("documentationTools")
    main = "src.com.openexchange.processor.generator.Main"
    args(arguments)

}

//TODO Refactor to Gradle plugin
tasks.register("insertMarkdown", JavaExec::class.java) {
    dependsOn("buildHtml")
    workingDir("../../documentation-generic")

    val arguments = mutableListOf(project.projectDir.name)
    classpath = configurations.getByName("documentationTools")
    main = "src.com.openexchange.replacer.Main"
    args(arguments)
}

sourceSets {
    main {
        java {
            srcDir(file(openApiGenerate.get().outputDir.get()).resolve("src/main/java"))
        }
    }
    test {
        java {
            srcDir(file(openApiGenerate.get().outputDir.get()).resolve("src/test/java"))
        }
    }
}

tasks.named("compileJava", JavaCompile::class.java) {
    dependsOn(openApiGenerate)
}

// TODO
//   - replace openexchange-test lib
//   - publish

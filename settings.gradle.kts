pluginManagement {
    repositories {
        maven {
            url = uri("https://artifactory.production.cloud.oxoe.io/artifactory/libs-release")
        }
    }
}

rootProject.name = "backend"

buildscript {
    repositories {
        maven {
            url = uri("https://artifactory.production.cloud.oxoe.io/artifactory/libs-release")
        }
    }
    dependencies {
        classpath("com.openexchange.build:projectset:1.2.0")
    }
}

// Just for testing some newly developed plugin feature
//includeBuild("../../engineering/gradle/<plugin>")

apply {
    plugin("com.openexchange.build.projectset")
}

include("com.openexchange.test")

include(":drive-api")
project(":drive-api").projectDir = File("http-api/drive_api")
include("http-api")
project(":http-api").projectDir = File("http-api/http_api")
include("rest-api")
project(":rest-api").projectDir = File("http-api/rest_api")
include("openexchange-test")

include("documentation") // subdirectory command_line_tools provides man pages

include("properties-documentation")
project(":properties-documentation").projectDir = File("documentation-generic/config")

// TODO enable later when JARs will be removed from com.openexchange.bundles
// includeBuild("target-platform")

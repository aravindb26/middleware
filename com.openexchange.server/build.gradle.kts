dependencies {
    // TODO workaround for missing interface org.bouncycastle.util.Encodable needed for compilation
    implementation(files("../com.openexchange.bundles/jars/bcprov-jdk15on-165-custom.jar"))
}

install {
    target("conf") {
        from("conf") {
            exclude("ox-scriptconf.sh.in")
        }
        into(prefixResolve("etc"))
    }
    target("templates") {
        from("templates")
        into(prefixResolve("templates"))
    }
    patch("ox-scriptconf.sh") {
        from("conf") {
            include("ox-scriptconf.sh.in")
        }
        into("conf")
    }
    templates {
        register("@libDir@") {
            buildConfig = com.openexchange.build.install.templating.BuildConfig.PATH
            path = "lib"
        }
        register("@propertiesdir@") {
            buildConfig = com.openexchange.build.install.templating.BuildConfig.PATH
            path = "etc"
        }
        register("@oxgroupwaresysconfdir@") {
             buildConfig = com.openexchange.build.install.templating.BuildConfig.PREFIX
        }
    }
    target("ox-scriptconf.sh") {
        from(buildDir.resolve("conf"))
        into(prefixResolve("etc"))
    }
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

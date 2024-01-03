install {
    // TODO we need to overwrite the bundle install convention due to sources.jar and javadoc.jar in lib directory and we don't want them
    // to be installed.
    target("bundle") {
        into(prefixResolve("bundles"))
        from("lib") {
            into("${project.name}/lib/")
            exclude("*-sources.jar", "*-javadoc.jar")
        }
        from(buildDir.resolve("META-INF/MANIFEST.MF")) {
            into("${project.name}/META-INF/")
        }
        from(buildDir.resolve("libs")) {
            into(project.name)
        }
    }
}

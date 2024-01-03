val copyLogo = tasks.register("copyJavadocLogo", Copy::class.java) {
    from("javadoc")
    into(buildDir.resolve("docs/javadoc"))
}

val javadoc = tasks.withType<Javadoc> {
    // FIXME source code needs to be fixed to not make javadoc fail
    isFailOnError = false
    title = "Open-Xchange Administration Interface"
    options {
        header("<img src=\"{@docRoot}/resources/OX_Logo.jpg\">")
    }
    finalizedBy(copyLogo)
}

tasks.getByName("assemble").dependsOn(javadoc)

install {
    symlink(prefixResolve("lib/${project.name}.jar"), prefixResolve("bundles/${project.name}.jar"))
}

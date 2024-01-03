install {
    target("javadoc") {
        from(rootProject.childProjects["com.openexchange.admin.reseller.rmi"]?.buildDir?.resolve("docs/javadoc"))
        into("/usr/share/doc/${project.name}/javadoc")
    }
}

packaging {
    copyright("server")
}

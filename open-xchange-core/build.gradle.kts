packaging {
    copyright("server")
}

install {
    target("docExamples") {
        from(rootProject.files("com.openexchange.server/doc/examples",
                "com.openexchange.database/doc/examples",
                "com.openexchange.authentication.application.impl/doc/examples")
        )
        into("/usr/share/doc/${project.name}/examples")
    }
    target("license text files") {
        from("docs")
        into("/usr/share/doc/${project.name}/docs")
    }
    // TODO this should be along with some bundle
    target("spool") {
        createEmptyDir("/var/spool/open-xchange/uploads")
    }
    // TODO place this somewhere at some logging bundle
    target("log") {
        createEmptyDir("/var/log/open-xchange")
    }
}

install {
    target("docs") {
        from("docs")
        into("/usr/share/doc/${project.name}/examples")
    }
}

packaging {
    copyright("server")
    upload {
        files.from("open-xchange.service")
        files.from("open-xchange.init")
    }
}

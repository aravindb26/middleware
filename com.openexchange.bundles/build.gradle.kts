osgi {
    exclude("org\\.eclipse\\.osgi_.*")
}

install {
    target("docs") {
        from("docs")
        into("/usr/share/doc/open-xchange-osgi/docs")
    }
}

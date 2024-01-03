install {
    target("templates") {
        from(project.files("templates"))
        into(prefixResolve("templates"))
    }
}

sourceSets {
    test {
        resources {
            srcDirs("test")
        }
    }
}

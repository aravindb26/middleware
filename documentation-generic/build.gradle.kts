install {
    target("configDocs") {
        from("config") {
            include("*.yml")
            exclude("template.yml")
        }
        into(prefixResolve("documentation/etc"))
    }
}

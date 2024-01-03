install {
    templates {
        register("@confDir@") {
            buildConfig = com.openexchange.build.install.templating.BuildConfig.PATH
            path = "etc"
        }
    }
    patch("conf") {
        from("conf")
        into("conf")
    }
    target("conf") {
        from(buildDir.resolve("conf"))
        into(prefixResolve("etc"))
    }
}

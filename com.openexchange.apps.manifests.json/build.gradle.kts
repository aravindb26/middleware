install {
    target("conf") {
        from("conf") {
            exclude("*.in")
        }
        into(prefixResolve("etc"))
    }
    templates {
        // @prefix@ exists by default
    }
}

install {
    target("oxfunctions") {
        from("lib")
        into(prefixResolve("lib"))
    }
    target("sbin") {
        from("sbin")
        into(prefixResolve("sbin"))
    }
}

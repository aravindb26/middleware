val resellerLinks = listOf(
        "deleteadmin",
        "changeadmin",
        "listadmin",
        "listrestrictions",
        "initrestrictions",
        "removerestrictions",
        "updatemoduleaccessrestrictions",
        "updaterestrictions")
install {
    resellerLinks.forEach { resellerLink ->
        symlink(prefixResolve("sbin/$resellerLink"), prefixResolve("sbin/createadmin"))
    }

    // TODO: Why the heck are there other files inside the sbin directory?!?
    patch("sbin") {
        from("sbin")
        include("*.in")
        into("sbin")
    }
}

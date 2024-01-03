install {
    val oauthLinks = listOf<String>("enableoauthclient","disableoauthclient","getoauthclient","listoauthclient","removeoauthclient","revokeoauthclient","updateoauthclient")
    oauthLinks.forEach{ oauthLink ->
        symlink(prefixResolve("sbin/$oauthLink"), prefixResolve("sbin/createoauthclient"))
    }
}

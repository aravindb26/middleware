install {

    val contextLinks = listOf<String>("changecontext","deletecontext","disablecontext","disableallcontext","enablecontext","enableallcontext","checkcountsconsistency","existscontext","deleteinvisible","movecontextdatabase","movecontextfilestore","listcontext","listcontextsbydatabase","listcontextsbyfilestore","listquota","changefilestore","recalculatefilestoreusage","listdatabase","listdatabaseschema","checkdatabase","upgradeschemata","unblockdatabase","listfilestore","listserver","changeserver","registerdatabase","createschemas","deleteemptyschemas","registerfilestore","registerserver","unregisterdatabase","unregisterfilestore","unregisterserver","changedatabase","getaccesscombinationnameforcontext","getmoduleaccessforcontext","getadminid","getcontextcapabilities","getschemaname","createschema")
    val userLinks = listOf<String>("deleteuser","changeuser","listuser","listuserfilestores","listusersbyaliasdomain","existsuser","creategroup","deletegroup","changegroup","listgroup","createresource","deleteresource","changeresource","moveuserfilestore","movecontextfilestore2user","moveuserfilestore2context","movemasterfilestore2user","moveuserfilestore2master","listresource","getaccesscombinationnameforuser","getusercapabilities","changeaccessglobal")
    val reasonLinks = listOf<String>("listreason","deletereason")

    contextLinks.forEach{ contextLink ->
        symlink(prefixResolve("sbin/$contextLink"), prefixResolve("sbin/createcontext"))
    }

    userLinks.forEach{ userLink ->
        symlink(prefixResolve("sbin/$userLink"), prefixResolve("sbin/createuser"))
    }

    reasonLinks.forEach{ reasonLink ->
        symlink(prefixResolve("sbin/$reasonLink"), prefixResolve("sbin/createreason"))
    }
}

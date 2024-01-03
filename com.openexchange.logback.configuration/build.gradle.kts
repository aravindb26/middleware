import com.openexchange.build.install.tasks.ReplaceRegexTask

pdeJava {
    installBundleAsDirectory.set(true)
}

val patchBundleManifest by tasks.existing(ReplaceRegexTask::class) {
    // we want this task to copy MANIFEST.MF to build folder but not to modify its contents
    this.replacements.set(emptyMap())
}

install {
    symlink(prefixResolve("bundles/${project.name}/logback.xml"), prefixResolve("etc/logback.xml"))
}

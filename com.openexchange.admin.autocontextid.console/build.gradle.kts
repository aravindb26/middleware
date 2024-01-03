import com.openexchange.build.ProjectType
import com.openexchange.build.install.convention.openexchange.OXCLTInstallConvention

projectType {
    type.set(ProjectType.CLT)
}

plugins {
    id("com.openexchange.build.pde-java")
}

install {
    // TODO setting the project type should be enough
    this.applyConvention(OXCLTInstallConvention(project, rootProject.install.prefix))
}

dependencies {
    implementation(project(":com.openexchange.admin.console.common"))
    implementation(project(":com.openexchange.admin.rmi"))
}


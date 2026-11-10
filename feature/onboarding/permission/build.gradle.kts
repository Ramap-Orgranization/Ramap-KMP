plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.designsystem)
        implementation(projects.core.platform)
        implementation(projects.core.ui)
    }
}

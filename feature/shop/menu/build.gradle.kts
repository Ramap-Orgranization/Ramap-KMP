plugins {
    id("ramap.kmp.feature")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.ui)
        implementation(projects.core.platform)
        implementation(projects.core.preview)
        implementation(libs.coil.compose)
    }
    sourceSets.commonTest.dependencies { implementation(projects.core.testing) }
}

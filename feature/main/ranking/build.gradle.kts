plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.preview)
        implementation(projects.core.analytics)
        implementation(libs.coil.compose)
    }
}

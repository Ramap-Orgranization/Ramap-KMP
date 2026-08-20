plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.analytics)
        implementation(projects.core.platform)
        implementation(libs.compose.foundation)
        implementation(libs.coil.compose)
    }
}

plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.analytics)
        implementation(projects.core.platform)
        implementation(projects.core.preview)
        implementation(libs.kotlinx.datetime)
        implementation(libs.navigation.event.compose)
    }
}

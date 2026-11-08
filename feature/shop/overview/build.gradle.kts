plugins {
    id("ramap.kmp.feature")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.platform)
        implementation(projects.core.navigation)
        implementation(projects.core.analytics)
        implementation(projects.core.preview)
        implementation(libs.kotlinx.datetime)
        implementation(libs.compose.ui.tooling.preview)
    }
    sourceSets.commonTest.dependencies { implementation(projects.core.testing) }
}

plugins {
    id("ramap.kmp.feature")
    id("ramap.kmp.test")
    id("ramap.koin")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.domain)
        implementation(projects.core.preview)
        implementation(projects.core.analytics)
        implementation(projects.core.ui)
        implementation(projects.core.designsystem)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.compose.material3)
        implementation(libs.compose.components.resources)
        implementation(libs.coil.compose)
    }
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
}

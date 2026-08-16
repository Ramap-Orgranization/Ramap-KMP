plugins {
    id("ramap.kmp.feature")
    id("ramap.kmp.test")
    id("ramap.koin")
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
    sourceSets.commonMain.dependencies {
        implementation(projects.domain)
        implementation(projects.core.analytics)
        implementation(projects.core.ui)
        implementation(projects.core.designsystem)
        implementation(projects.core.platform)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.components.resources)
        implementation(libs.coil.compose)
    }
}

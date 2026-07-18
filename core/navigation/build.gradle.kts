plugins {
    id("ramap.kmp.compose")
    id("ramap.kmp.test")
    id("ramap.serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.designsystem)
        api(libs.androidx.navigation3.runtime)
        implementation(libs.androidx.navigation3.ui)
        implementation(libs.androidx.lifecycle.viewmodel.navigation3)
        implementation(libs.compose.material3)
        implementation(libs.compose.components.resources)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.navigation.event.compose)
    }
}

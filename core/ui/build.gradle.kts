plugins { id("ramap.kmp.compose") }

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.common)
        api(projects.domain)
        implementation(projects.core.designsystem)
        implementation(libs.compose.components.resources)
        implementation(libs.androidx.lifecycle.viewmodel.compose)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kermit)
    }
}

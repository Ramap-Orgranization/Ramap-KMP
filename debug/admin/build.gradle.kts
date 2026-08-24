plugins {
    id("ramap.kmp.compose")
    id("ramap.serialization")
}

kotlin {
    androidLibrary {
        androidResources {
            enable = true
        }
    }

    sourceSets.androidMain.dependencies {
        implementation(projects.core.network)
        implementation(projects.core.designsystem)
        implementation(projects.core.ui)
        implementation(projects.domain)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.coil.compose)
        implementation(libs.compose.components.resources)
        implementation(libs.compose.material3)
        implementation(libs.supabase.functions)
        implementation(libs.supabase.postgrest)
        implementation(libs.supabase.storage)
        implementation(libs.ktor.client.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.android)
        implementation(libs.koin.compose.viewmodel)
    }
}

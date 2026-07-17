plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
    id("ramap.serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kermit)
        implementation(libs.supabase.auth)
        implementation(libs.supabase.postgrest)
    }
}

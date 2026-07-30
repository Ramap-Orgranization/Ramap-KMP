plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
    id("ramap.serialization")
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
    sourceSets.commonMain.dependencies {
        api(projects.domain)
        implementation(projects.core.network)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
        implementation(libs.supabase.auth)
        implementation(libs.supabase.functions)
        implementation(libs.supabase.postgrest)
        implementation(libs.ktor.client.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.coroutines.core)
    }
    sourceSets.androidMain {
        dependencies { implementation(libs.kakao.user) }
    }
}

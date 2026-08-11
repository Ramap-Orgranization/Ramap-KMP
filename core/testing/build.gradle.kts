plugins {
    id("ramap.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.analytics)
        api(projects.domain)
        api(libs.kotlinx.coroutines.test)
        implementation(libs.kotlinx.datetime)
    }
}

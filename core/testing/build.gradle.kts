plugins {
    id("ramap.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.domain)
        api(libs.kotlinx.coroutines.test)
    }
}

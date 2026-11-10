plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.datetime)
        implementation(libs.kotlinx.coroutines.core)
    }
}

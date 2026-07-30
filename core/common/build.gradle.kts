plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
    }
}

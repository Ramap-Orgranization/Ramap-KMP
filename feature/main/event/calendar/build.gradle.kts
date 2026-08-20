plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.datetime)
    }
}

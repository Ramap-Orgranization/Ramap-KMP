plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.coil.compose)
    }
}

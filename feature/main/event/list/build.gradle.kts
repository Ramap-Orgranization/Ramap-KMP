plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.analytics)
        implementation(libs.coil.compose)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.activity.compose)
    }
}

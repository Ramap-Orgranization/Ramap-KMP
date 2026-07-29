plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
    id("ramap.koin")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kermit)
    }
    sourceSets.androidMain.dependencies {
        implementation(project.dependencies.platform(libs.firebase.bom))
        implementation(libs.firebase.analytics)
        implementation(libs.firebase.crashlytics)
    }
}

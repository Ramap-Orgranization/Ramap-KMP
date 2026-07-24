plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.core.navigation)
        implementation(libs.kermit)
    }
    sourceSets.androidMain.dependencies {
        implementation(project.dependencies.platform(libs.firebase.bom))
        implementation(libs.firebase.analytics)
        implementation(libs.firebase.crashlytics)
    }
}

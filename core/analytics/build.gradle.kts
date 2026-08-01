plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
    id("ramap.koin")
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
    sourceSets.commonMain.dependencies {
        implementation(libs.kermit)
    }
    sourceSets.androidMain.dependencies {
        implementation(project.dependencies.platform(libs.firebase.bom))
        implementation(libs.firebase.analytics)
        implementation(libs.firebase.crashlytics)
    }
}

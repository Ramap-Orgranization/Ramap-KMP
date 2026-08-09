plugins { id("ramap.kmp.compose") }

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.androidx.datastore.preferences)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.activity)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.core)
        implementation(libs.play.services.location)
    }
}

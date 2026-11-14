plugins {
    id("ramap.kmp.compose")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.jindong.core)
    }
    sourceSets.androidHostTest.dependencies {
        implementation(libs.robolectric)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.activity)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.core)
        implementation(libs.play.app.update)
        implementation(libs.play.services.location)
    }
}

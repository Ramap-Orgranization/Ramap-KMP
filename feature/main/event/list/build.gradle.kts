import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("ramap.kmp.feature")
}

kotlin {
    targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
        androidResources {
            enable = true
        }
        compilations.withType(KotlinMultiplatformAndroidHostTestCompilation::class.java).configureEach {
            isIncludeAndroidResources = true
        }
    }
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.compose.ui.test)
        implementation(libs.navigation.event.compose)
        implementation(libs.robolectric)
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.core.analytics)
        implementation(libs.coil.compose)
    }
}

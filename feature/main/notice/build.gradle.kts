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

    sourceSets.commonMain.dependencies {
        implementation(projects.core.analytics)
        implementation(projects.core.platform)
        implementation(projects.core.preview)
        implementation(libs.kotlinx.datetime)
        implementation(libs.navigation.event.compose)
    }
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.compose.ui.test)
        implementation(libs.robolectric)
    }
}

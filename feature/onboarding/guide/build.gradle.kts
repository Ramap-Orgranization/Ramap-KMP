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
        implementation(projects.feature.onboarding.permission)
        implementation(libs.androidx.activity.compose)
        implementation(libs.compose.ui.test)
        implementation(libs.navigation.event.compose)
        implementation(libs.robolectric)
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.core.analytics)
        implementation(projects.core.designsystem)
        implementation(projects.core.ui)
        implementation(projects.feature.onboarding.shopSelection)
    }
}

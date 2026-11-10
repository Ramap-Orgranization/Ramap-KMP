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
        implementation(libs.robolectric)
    }

    sourceSets.commonMain.dependencies {
        implementation(libs.kermit)
        implementation(projects.domain)
        implementation(projects.core.analytics)
        implementation(projects.core.designsystem)
        implementation(projects.core.platform)
        implementation(projects.core.preview)
        implementation(projects.core.ui)
    }

    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.turbine)
    }
}

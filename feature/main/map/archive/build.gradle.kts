import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    id("ramap.kmp.feature")
    kotlin("native.cocoapods")
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
    cocoapods {
        summary = "Archive map feature for Ramap"
        homepage = "https://github.com/chanho0908/Ramap-kmp"
        version = "1.0.0"
        ios.deploymentTarget = "13.0"
        pod("NMapsMap") {
            packageName = "com.peto.ramap.ui.main.map.archive.nmaps"
        }
    }
    sourceSets.commonMain.dependencies {
        implementation(projects.core.platform)
        implementation(libs.coil.compose)
        implementation(projects.core.preview)
        implementation(libs.navigation.event.compose)
        implementation(libs.balloon)
        implementation(libs.kotlinx.datetime)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.naver.map)
        implementation(libs.play.services.location)
    }
    sourceSets.androidHostTest.dependencies {
        implementation(libs.robolectric)
        implementation(libs.compose.ui.test)
        implementation(libs.androidx.activity.compose)
    }
}

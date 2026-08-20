plugins {
    id("ramap.kmp.feature")
    kotlin("native.cocoapods")
}

kotlin {
    cocoapods {
        summary = "Map feature for Ramap"
        homepage = "https://github.com/chanho0908/Ramap-kmp"
        version = "1.0.0"
        ios.deploymentTarget = "13.0"
        pod("NMapsMap")
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.core.platform)
        implementation(projects.core.navigation)
        implementation(libs.kotlinx.datetime)
    }
    sourceSets.commonMain {
        dependencies {
            implementation(projects.core.analytics)
            implementation(projects.core.preview)
            implementation(libs.navigation.event.compose)
            implementation(libs.coil.compose)
            implementation(libs.compose.ui.tooling.preview)
        }
    }
    sourceSets.androidMain {
        dependencies {
            implementation(libs.naver.map)
            implementation(libs.play.services.location)
            implementation(libs.compose.ui.tooling.preview)
        }
    }
}

plugins {
    id("ramap.kmp.feature")
    id("ramap.kmp.test")
    id("ramap.koin")
    kotlin("native.cocoapods")
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
    cocoapods {
        summary = "Map feature for Ramap"
        homepage = "https://github.com/chanho0908/Ramap-kmp"
        version = "1.0.0"
        ios.deploymentTarget = "13.0"
        pod("NMapsMap")
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.domain)
        implementation(projects.core.designsystem)
        implementation(projects.core.platform)
        implementation(projects.core.navigation)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.compose.material3)
        implementation(libs.compose.components.resources)
    }
    sourceSets.commonMain {
        dependencies {
            implementation(projects.core.analytics)
            implementation(projects.core.ui)
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

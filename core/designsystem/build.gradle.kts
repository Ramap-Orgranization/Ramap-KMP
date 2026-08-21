plugins {
    id("ramap.kmp.compose")
    id("ramap.kmp.test")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "ramap.shared.generated.resources"
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }

    androidLibrary {
        androidResources {
            enable = true
        }
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.core.preview)
        implementation(projects.domain)
        implementation(libs.compose.material3)
        implementation(libs.compose.components.resources)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor3)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
        implementation(libs.navigation.event.compose)
        implementation(libs.kotlinx.datetime)
    }
}

plugins { id("ramap.kmp.compose") }

compose.resources {
    publicResClass = true
    packageOfResClass = "ramap.shared.generated.resources"
}

kotlin {
    androidLibrary {
        androidResources {
            enable = true
        }
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.domain)
        implementation(libs.compose.material3)
        implementation(libs.compose.components.resources)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor3)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
        implementation(libs.navigation.event.compose)
    }
}

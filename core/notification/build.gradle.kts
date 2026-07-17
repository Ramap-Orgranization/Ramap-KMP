plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
    sourceSets.commonMain.dependencies {
        implementation(projects.domain)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kermit)
    }
}

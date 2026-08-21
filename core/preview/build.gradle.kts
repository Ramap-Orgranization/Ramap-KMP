plugins {
    id("ramap.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.domain)
        implementation(libs.kotlinx.datetime)
    }
}

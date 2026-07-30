plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(libs.kotlinx.coroutines.core)
            }
        }
    }
}

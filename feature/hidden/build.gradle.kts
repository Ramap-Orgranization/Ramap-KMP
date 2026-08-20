plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.analytics)
            }
        }
    }
}

plugins {
    id("ramap.kmp.feature")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.core.preview)
            implementation(projects.core.analytics)
        }
    }
}

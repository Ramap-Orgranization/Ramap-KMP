plugins {
    id("ramap.kmp.feature")
    id("ramap.kmp.test")
}

kotlin {
    sourceSets.commonTest.dependencies { implementation(projects.core.testing) }
}

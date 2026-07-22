plugins {
    id("ramap.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.feature.main.events)
        api(projects.feature.main.map)
        api(projects.feature.main.ranking)
        api(projects.feature.main.my)
    }
}

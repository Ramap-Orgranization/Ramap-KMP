plugins {
    id("ramap.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.feature.main.event.list)
        api(projects.feature.main.event.detail)
        api(projects.feature.main.event.calendar)
        api(projects.feature.main.map)
        api(projects.feature.main.ranking)
        api(projects.feature.main.my)
    }
}

rootProject.name = "Ramap"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://repository.map.naver.com/archive/maven")
        maven("https://devrepo.kakao.com/nexus/content/groups/public/")
    }
}

include(":androidApp")
include(":shared")
include(":core:common")
include(":core:ui")
include(":core:designsystem")
include(":core:network")
include(":core:platform")
include(":core:notification")
include(":core:navigation")
include(":core:testing")
include(":domain")
include(":data")
include(":feature:main")
include(":feature:main:events")
include(":feature:main:map")
include(":feature:main:my")
include(":feature:event:detail")
include(":feature:account")
include(":feature:bookmark")
include(":feature:hidden")
include(":feature:notification")
include(":feature:report")
include(":feature:subscribed")

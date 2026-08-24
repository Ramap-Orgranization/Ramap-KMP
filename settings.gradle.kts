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
include(":core:analytics")
include(":core:common")
include(":core:ui")
include(":core:preview")
include(":core:designsystem")
include(":core:network")
include(":core:platform")
include(":core:notification")
include(":core:navigation")
include(":core:testing")
include(":domain")
include(":data")
include(":debug:admin")
include(":feature:main")
include(":feature:main:event:list")
include(":feature:main:event:detail")
include(":feature:main:event:calendar")
include(":feature:main:notice")
include(":feature:main:map")
include(":feature:main:ranking")
include(":feature:main:my")
include(":feature:account")
include(":feature:bookmark:list")
include(":feature:bookmark:importation")
include(":feature:hidden")
include(":feature:notification")
include(":feature:report")
include(":feature:subscribed")

plugins {
    `kotlin-dsl`
}

group = "com.peto.ramap.buildlogic"

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "ramap.kmp.library"
            implementationClass = "com.peto.ramap.convention.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "ramap.kmp.compose"
            implementationClass = "com.peto.ramap.convention.KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "ramap.kmp.feature"
            implementationClass = "com.peto.ramap.convention.KmpFeatureConventionPlugin"
        }
        register("kmpTest") {
            id = "ramap.kmp.test"
            implementationClass = "com.peto.ramap.convention.KmpTestConventionPlugin"
        }
        register("koin") {
            id = "ramap.koin"
            implementationClass = "com.peto.ramap.convention.KoinConventionPlugin"
        }
        register("serialization") {
            id = "ramap.serialization"
            implementationClass = "com.peto.ramap.convention.SerializationConventionPlugin"
        }
        register("androidApplication") {
            id = "ramap.android.application"
            implementationClass = "com.peto.ramap.convention.AndroidApplicationConventionPlugin"
        }
    }
}

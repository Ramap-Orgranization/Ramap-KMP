import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("ramap.serialization")
    kotlin("native.cocoapods")
}

compose.resources {
    generateResClass = never
    packageOfResClass = "com.peto.ramap.shared.resources"
}

kotlin {
    cocoapods {
        summary = "Shared module for Ramap"
        homepage = "https://github.com/chanho0908/Ramap-kmp"
        version = "1.0.0"
        ios.deploymentTarget = "13.0"

        framework {
            baseName = "Shared"
            isStatic = true
            export(projects.core.network)
        }

        pod("NMapsMap")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.core.network)
        }
    }

    androidLibrary {
        namespace = "com.peto.ramap.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity)
            implementation(libs.androidx.core)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.naver.map)
            implementation(libs.play.services.location)
            implementation(libs.kotlinx.coroutines.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(projects.core.designsystem)
            api(projects.core.network)
            implementation(projects.core.platform)
            implementation(projects.core.notification)
            implementation(projects.core.navigation)
            implementation(projects.domain)
            implementation(projects.data)
            implementation(projects.feature.main)
            implementation(projects.feature.account)
            implementation(projects.feature.bookmark)
            implementation(projects.feature.hidden)
            implementation(projects.feature.notification)
            implementation(projects.feature.report)
            implementation(projects.feature.subscribed)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Supabase
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)

            // Ktor & Serialization
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.compose.ui.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

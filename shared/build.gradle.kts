import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use(::load)
        }
    }

fun secretProperty(
    localName: String,
    envName: String,
): String =
    providers
        .gradleProperty(localName)
        .orElse(providers.environmentVariable(envName))
        .orNull
        ?: localProperties.getProperty(localName).orEmpty()

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.buildkonfig)
    kotlin("native.cocoapods")
}

buildkonfig {
    packageName = "com.peto.ramap.shared"
    objectName = "RamapConfig"

    defaultConfigs {
        buildConfigField(
            STRING,
            "SUPABASE_URL",
            secretProperty(
                localName = "supabase.url",
                envName = "SUPABASE_URL",
            ),
        )
        buildConfigField(
            STRING,
            "SUPABASE_ANON_KEY",
            secretProperty(
                localName = "supabase.anon_key",
                envName = "SUPABASE_ANON_KEY",
            ),
        )
        buildConfigField(
            STRING,
            "KAKAO_NATIVE_APP_KEY",
            secretProperty(
                localName = "kakao_native_app_key",
                envName = "KAKAO_NATIVE_APP_KEY",
            ),
        )
        buildConfigField(
            STRING,
            "NAVER_MAP_NCP_KEY_ID",
            secretProperty(
                localName = "naver_map_ncp_key_id",
                envName = "NAVER_MAP_NCP_KEY_ID",
            ),
        )
    }
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
        }

        pod("KakaoMapsSDK") {
            version = "2.12.14"
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigationevent.compose)

            // Supabase
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.auth)

            // Ktor & Serialization
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)

            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // DataStore & Okio
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.okio)
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

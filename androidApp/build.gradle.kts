import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties =
    Properties().apply {
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use(::load)
    }
val kakaoNativeAppKey =
    providers
        .gradleProperty("kakao_native_app_key")
        .orElse(providers.environmentVariable("KAKAO_NATIVE_APP_KEY"))
        .orNull
        ?: localProperties.getProperty("kakao_native_app_key").orEmpty()

fun releaseProperty(
    localName: String,
    envName: String,
): String? =
    providers
        .gradleProperty(localName)
        .orElse(providers.environmentVariable(envName))
        .orNull
        ?: localProperties.getProperty(localName)

val releaseStoreFile = releaseProperty("release.store.file", "RELEASE_STORE_FILE")
val releaseStorePassword = releaseProperty("release.store.password", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseProperty("release.key.alias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseProperty("release.key.password", "RELEASE_KEY_PASSWORD")
val hasReleaseSigning =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)
    implementation(libs.kakao.map)
    implementation(libs.kakao.user)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.peto.ramap"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.peto.ramap"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

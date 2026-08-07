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
    id("ramap.android.application")
}

if (file("google-services.json").exists()) {
    apply(
        plugin =
            libs.plugins.google.services
                .get()
                .pluginId,
    )
    apply(
        plugin =
            libs.plugins.firebase.crashlytics
                .get()
                .pluginId,
    )
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)
    implementation(projects.core.designsystem)
    implementation(projects.core.analytics)
    implementation(projects.core.network)
    implementation(projects.core.notification)
    implementation(projects.core.navigation)
    implementation(projects.core.platform)
    implementation(projects.data)
    implementation(libs.naver.map)
    implementation(libs.kakao.user)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.install.referrer)
    implementation(libs.kermit)

    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}

android {
    namespace = "com.peto.ramap"
    compileSdk =
        libs.versions.android.compile.sdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.peto.ramap"
        minSdk =
            libs.versions.android.min.sdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.target.sdk
                .get()
                .toInt()
        versionCode = 11
        versionName = "1.0.10"
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
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.configureEach {
    if (name == "mergeDebugAssets" || name == "mergeReleaseAssets") {
        dependsOn(":core:designsystem:copyAndroidMainComposeResourcesToAndroidAssets")
    }
}

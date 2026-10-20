import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import java.util.Properties

plugins {
    id("ramap.kmp.library")
    id("ramap.kmp.test")
    id("ramap.serialization")
    alias(libs.plugins.build.konfig)
}

val localProperties =
    Properties().apply {
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use(::load)
    }

fun secretProperty(
    localName: String,
    envName: String,
): String =
    providers.gradleProperty(localName).orElse(providers.environmentVariable(envName)).orNull
        ?: localProperties.getProperty(localName).orEmpty()

buildkonfig {
    packageName = "com.peto.ramap.network.config"
    objectName = "RamapConfig"
    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_URL", secretProperty("supabase.url", "SUPABASE_URL"))
        buildConfigField(STRING, "SUPABASE_ANON_KEY", secretProperty("supabase.anon_key", "SUPABASE_ANON_KEY"))
        buildConfigField(STRING, "KAKAO_NATIVE_APP_KEY", secretProperty("kakao_native_app_key", "KAKAO_NATIVE_APP_KEY"))
        buildConfigField(STRING, "NAVER_MAP_NCP_KEY_ID", secretProperty("naver_map_ncp_key_id", "NAVER_MAP_NCP_KEY_ID"))
        buildConfigField(
            STRING,
            "SHOP_LINK_BASE_URL",
            secretProperty("shop.link.base_url", "SHOP_LINK_BASE_URL").ifBlank { "https://ramap-link.vercel.app" },
        )
        buildConfigField(
            STRING,
            "SHOP_LINK_WEB_HOST",
            secretProperty("shop.link.web_host", "SHOP_LINK_WEB_HOST").ifBlank { "ramap-link.vercel.app" },
        )
    }
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.common)
        api(projects.domain)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.core)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.supabase.auth)
        implementation(libs.supabase.functions)
        implementation(libs.supabase.postgrest)
        implementation(libs.supabase.storage)
        implementation(libs.kermit)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.ktor.client.okhttp)
        implementation(libs.koin.android)
    }
    sourceSets.androidHostTest.dependencies { implementation(libs.robolectric) }
    sourceSets.iosMain.dependencies { implementation(libs.ktor.client.darwin) }
}

// 독립 실행 테스트에는 Keychain 서비스가 없습니다 (KT-61470).
// 부팅된 시뮬레이터에서 통합 검증: :core:network:iosSimulatorArm64Test -Pramap.keychainTestDevice=<UDID>
val keychainTestDevice = providers.gradleProperty("ramap.keychainTestDevice")
if (keychainTestDevice.isPresent) {
    kotlin.iosSimulatorArm64().testRuns.configureEach {
        executionSource.binary.linkerOpts(
            "-sectcreate",
            "__TEXT",
            "__entitlements",
            file("src/iosTest/resources/keychain-test-entitlements.plist").absolutePath,
        )
    }
}
tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    if (keychainTestDevice.isPresent) {
        standalone.set(false)
        device.set(keychainTestDevice)
    } else {
        filter.excludeTestsMatching("com.peto.ramap.network.auth.IosKeychainSessionStorageIntegrationTest.*")
    }
}

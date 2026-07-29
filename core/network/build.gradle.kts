import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
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
        buildConfigField(STRING, "NAVER_CLIENT_SECRET", secretProperty("naver_client_secret", "NAVER_CLIENT_SECRET"))
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
    }
    sourceSets.androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
    sourceSets.iosMain.dependencies { implementation(libs.ktor.client.darwin) }
}

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties

plugins {
    id("ramap.kmp.compose")
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

fun adminCredential(
    localName: String,
    envName: String,
): String =
    providers.gradleProperty(localName).orElse(providers.environmentVariable(envName)).orNull
        ?: localProperties.getProperty(localName).orEmpty()

buildkonfig {
    packageName = "com.peto.ramap.debug.admin.config"
    objectName = "AdminConfig"
    defaultConfigs {
        buildConfigField(STRING, "ADMIN_EMAIL", adminCredential("admin.email", "RAMAP_ADMIN_EMAIL"))
        buildConfigField(STRING, "ADMIN_PASSWORD", adminCredential("admin.password", "RAMAP_ADMIN_PASSWORD"))
    }
}

kotlin {
    androidLibrary {
        androidResources {
            enable = true
        }
    }

    sourceSets.androidMain.dependencies {
        implementation(projects.core.network)
        implementation(projects.core.designsystem)
        implementation(projects.core.ui)
        implementation(projects.domain)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.coil.compose)
        implementation(libs.compose.components.resources)
        implementation(libs.compose.material3)
        implementation(libs.supabase.functions)
        implementation(libs.supabase.auth)
        implementation(libs.supabase.postgrest)
        implementation(libs.supabase.storage)
        implementation(libs.ktor.client.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.android)
        implementation(libs.koin.compose.viewmodel)
    }

    sourceSets.androidHostTest.dependencies {
        implementation(projects.core.testing)
    }
}

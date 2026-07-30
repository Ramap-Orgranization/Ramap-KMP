package com.peto.ramap.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(pluginId("kotlin-multiplatform"))
        pluginManager.apply(pluginId("android-multiplatform-library"))

        extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()
            targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
                namespace = "com.peto.ramap.${path.removePrefix(":").replace(':', '.').replace('-', '_')}"
                compileSdk = libs.findVersion("android-compile-sdk").get().requiredVersion.toInt()
                minSdk = libs.findVersion("android-min-sdk").get().requiredVersion.toInt()
                compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }
}

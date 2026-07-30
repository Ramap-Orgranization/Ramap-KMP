package com.peto.ramap.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(pluginId("android-application"))
        pluginManager.apply(pluginId("compose-multiplatform"))
        pluginManager.apply(pluginId("compose-compiler"))
        extensions.configure<ApplicationExtension> {
            compileSdk = libs.findVersion("android-compile-sdk").get().requiredVersion.toInt()
            defaultConfig {
                minSdk = libs.findVersion("android-min-sdk").get().requiredVersion.toInt()
                targetSdk = libs.findVersion("android-target-sdk").get().requiredVersion.toInt()
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
}

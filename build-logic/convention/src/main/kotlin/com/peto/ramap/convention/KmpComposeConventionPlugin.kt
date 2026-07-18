package com.peto.ramap.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("ramap.kmp.library")
        pluginManager.apply(pluginId("compose-multiplatform"))
        pluginManager.apply(pluginId("compose-compiler"))

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.findLibrary("compose-runtime").get())
                implementation(libs.findLibrary("compose-foundation").get())
                implementation(libs.findLibrary("compose-ui").get())
            }
        }
    }
}

package com.peto.ramap.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("ramap.kmp.compose")
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            }
        }
    }
}

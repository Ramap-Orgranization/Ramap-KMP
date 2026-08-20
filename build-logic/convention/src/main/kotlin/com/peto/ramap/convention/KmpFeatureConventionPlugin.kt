package com.peto.ramap.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("ramap.kmp.compose")
        pluginManager.apply("ramap.kmp.test")
        pluginManager.apply("ramap.koin")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":domain"))
                implementation(project(":core:ui"))
                implementation(project(":core:designsystem"))

                implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                implementation(libs.findLibrary("koin-compose-viewmodel").get())
                implementation(libs.findLibrary("compose-material3").get())
                implementation(libs.findLibrary("compose-components-resources").get())
            }

            sourceSets.getByName("commonTest").dependencies {
                implementation(project(":core:testing"))
            }
        }
    }
}

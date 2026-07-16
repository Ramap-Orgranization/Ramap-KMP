package com.peto.ramap.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("ramap.kmp.library")
        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
                withHostTest {}
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                implementation(libs.findLibrary("turbine").get())
            }
        }
    }
}

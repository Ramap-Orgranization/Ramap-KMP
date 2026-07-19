package com.peto.ramap.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

class SerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(pluginId("kotlinx-serialization"))
    }
}

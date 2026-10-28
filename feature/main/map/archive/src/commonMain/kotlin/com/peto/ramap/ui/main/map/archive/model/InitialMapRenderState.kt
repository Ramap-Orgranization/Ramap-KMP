package com.peto.ramap.ui.main.map.archive.model

internal class InitialMapRenderState {
    private var isCameraConfigured = false
    private var hasRendered = false

    fun cameraConfigured() {
        isCameraConfigured = true
    }

    fun onFrameRendered(
        fully: Boolean,
        stable: Boolean,
    ): Boolean {
        if (!isCameraConfigured || !fully || !stable || hasRendered) return false
        hasRendered = true
        return true
    }
}

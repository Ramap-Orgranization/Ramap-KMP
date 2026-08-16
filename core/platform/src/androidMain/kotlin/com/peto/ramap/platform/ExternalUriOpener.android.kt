package com.peto.ramap.platform

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

actual object ExternalUriOpener {
    actual val isAppleMapsAvailable: Boolean = false

    private var activity: Activity? = null
    private var appUpdateManager: AppUpdateManager? = null

    fun attach(activity: Activity) {
        this.activity = activity
        appUpdateManager = AppUpdateManagerFactory.create(activity)
    }

    fun detach(activity: Activity) {
        if (this.activity === activity) {
            this.activity = null
            appUpdateManager = null
        }
    }

    actual fun open(uri: String) {
        val normalizedUri = uri.trim()
        if (!isSupportedUri(normalizedUri)) return

        runCatching {
            activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUri)))
        }
    }

    actual fun startAppUpdate(uri: String) {
        val currentActivity = activity ?: return
        val updateManager = appUpdateManager ?: return open(uri)

        updateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                val canStartImmediateUpdate =
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                if (!canStartImmediateUpdate) {
                    open(uri)
                    return@addOnSuccessListener
                }

                runCatching {
                    updateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        currentActivity,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        UPDATE_REQUEST_CODE,
                    )
                }.onFailure { open(uri) }
            }.addOnFailureListener { open(uri) }
    }

    actual fun resumeAppUpdate() {
        val currentActivity = activity ?: return
        val updateManager = appUpdateManager ?: return

        updateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() != UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                return@addOnSuccessListener
            }

            runCatching {
                updateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    currentActivity,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                    UPDATE_REQUEST_CODE,
                )
            }
        }
    }

    actual fun isSupportedWebUri(uri: String): Boolean {
        val normalizedUri = uri.trim().lowercase()
        return normalizedUri.startsWith("https://") || normalizedUri.startsWith("http://")
    }

    actual fun openAppleMaps(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
    ) = Unit

    private fun isSupportedUri(uri: String): Boolean = isSupportedWebUri(uri) || uri.lowercase().startsWith("tel:")

    private const val UPDATE_REQUEST_CODE = 1001
}

package com.peto.ramap.platform

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

actual object NotificationPermissionRequester {
    actual val isSupported = true

    private var activityReference = WeakReference<Activity>(null)
    private var permissionContinuation: kotlin.coroutines.Continuation<Boolean>? = null

    fun attach(activity: Activity) {
        activityReference = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (activityReference.get() === activity) activityReference.clear()
    }

    actual suspend fun isGranted(): Boolean {
        val activity = activityReference.get() ?: return false
        val runtimePermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimePermissionGranted && NotificationManagerCompat.from(activity).areNotificationsEnabled()
    }

    actual suspend fun request(): Boolean {
        if (isGranted()) return true
        val activity = activityReference.get() ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
        return suspendCancellableCoroutine { continuation ->
            permissionContinuation?.resume(false)
            permissionContinuation = continuation
            continuation.invokeOnCancellation { permissionContinuation = null }
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
        }
    }

    fun complete(
        requestCode: Int,
        grantResults: IntArray,
    ) {
        if (requestCode != REQUEST_CODE) return
        val isGranted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        permissionContinuation?.resume(isGranted)
        permissionContinuation = null
    }

    private const val REQUEST_CODE = 2001
}

package com.peto.ramap.platform.permission

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat

internal class RecordingNotificationPermissionLauncher : ActivityResultLauncher<String>() {
    val requestedPermissions = mutableListOf<String>()

    override fun launch(
        input: String,
        options: ActivityOptionsCompat?,
    ) {
        requestedPermissions += input
    }

    override fun unregister() = Unit

    override val contract: ActivityResultContract<String, *> = ActivityResultContracts.RequestPermission()
}

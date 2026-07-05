package com.peto.ramap.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class AndroidAppSettingsOpener(
    private val context: Context,
) : AppSettingsOpener {
    override fun open() {
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }
}

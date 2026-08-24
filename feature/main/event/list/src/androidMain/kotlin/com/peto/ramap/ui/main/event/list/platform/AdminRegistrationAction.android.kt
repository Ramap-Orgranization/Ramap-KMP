package com.peto.ramap.ui.main.event.list.platform

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberAdminRegistrationAction(): (() -> Unit)? {
    val context = LocalContext.current
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (!isDebuggable) return null

    return remember(context) {
        {
            context.startActivity(
                Intent().setClassName(
                    context.packageName,
                    context.packageName + ADMIN_ACTIVITY_SUFFIX,
                ),
            )
        }
    }
}

private const val ADMIN_ACTIVITY_SUFFIX = ".admin.AdminRegistrationActivity"

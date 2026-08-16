package com.peto.ramap.platform

import android.content.Context
import android.os.Build

class AndroidAppVersionProvider(
    context: Context,
) : AppVersionProvider {
    private val packageInfo =
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
        }.getOrNull()

    override val versionName: String = packageInfo?.versionName.orEmpty()
    override val buildNumber: Long =
        when {
            packageInfo == null -> 0L
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> packageInfo.longVersionCode
            else -> packageInfo.versionCode.toLong()
        }
    override val platform: String = PLATFORM

    private companion object {
        const val PLATFORM = "android"
    }
}

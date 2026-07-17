package com.peto.ramap.platform

import android.content.Context

class AndroidAppVersionProvider(
    context: Context,
) : AppVersionProvider {
    override val versionName: String =
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }.getOrDefault("")
}

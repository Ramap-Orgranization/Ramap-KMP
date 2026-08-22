package com.peto.ramap.platform

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.peto.ramap.platform.storage.AppNoticeStorage
import com.peto.ramap.platform.storage.DefaultAppNoticeStorage
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

fun createAppNoticeStorage(): AppNoticeStorage =
    DefaultAppNoticeStorage(
        PreferenceDataStoreFactory.createWithPath {
            (NSHomeDirectory() + "/Library/Application Support/Ramap/app_notice.preferences_pb").toPath()
        },
    )

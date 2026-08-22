package com.peto.ramap.platform

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.peto.ramap.platform.storage.AppNoticeStorage
import com.peto.ramap.platform.storage.DefaultAppNoticeStorage

fun createAppNoticeStorage(context: Context): AppNoticeStorage =
    DefaultAppNoticeStorage(
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("app_notice.preferences_pb")
        },
    )

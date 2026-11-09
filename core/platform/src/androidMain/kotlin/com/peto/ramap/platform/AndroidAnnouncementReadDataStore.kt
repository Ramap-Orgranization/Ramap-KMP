package com.peto.ramap.platform

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.peto.ramap.platform.storage.AnnouncementReadStorage
import com.peto.ramap.platform.storage.impl.DefaultAnnouncementReadStorage

fun createAnnouncementReadStorage(context: Context): AnnouncementReadStorage =
    DefaultAnnouncementReadStorage(
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("announcement_read.preferences_pb")
        },
    )

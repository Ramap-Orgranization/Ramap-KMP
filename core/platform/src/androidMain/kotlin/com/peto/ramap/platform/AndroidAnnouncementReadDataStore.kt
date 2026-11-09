package com.peto.ramap.platform

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.peto.ramap.platform.storage.announcement.AnnouncementReadStorage
import com.peto.ramap.platform.storage.announcement.DefaultAnnouncementReadStorage

fun createAnnouncementReadStorage(context: Context): AnnouncementReadStorage =
    DefaultAnnouncementReadStorage(
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("announcement_read.preferences_pb")
        },
    )

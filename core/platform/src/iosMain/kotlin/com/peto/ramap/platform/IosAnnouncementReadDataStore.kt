package com.peto.ramap.platform

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.peto.ramap.platform.storage.announcement.AnnouncementReadStorage
import com.peto.ramap.platform.storage.announcement.DefaultAnnouncementReadStorage
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

fun createAnnouncementReadStorage(): AnnouncementReadStorage =
    DefaultAnnouncementReadStorage(
        PreferenceDataStoreFactory.createWithPath {
            (NSHomeDirectory() + "/Library/Application Support/Ramap/announcement_read.preferences_pb").toPath()
        },
    )

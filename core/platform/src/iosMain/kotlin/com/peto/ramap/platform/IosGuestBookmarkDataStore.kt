package com.peto.ramap.platform

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.peto.ramap.platform.storage.GuestBookmarkStorage
import com.peto.ramap.platform.storage.impl.DefaultGuestBookmarkStorage
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

fun createGuestBookmarkStorage(): GuestBookmarkStorage =
    DefaultGuestBookmarkStorage(
        PreferenceDataStoreFactory.createWithPath {
            (NSHomeDirectory() + "/Library/Application Support/Ramap/guest_bookmarks.preferences_pb").toPath()
        },
    )

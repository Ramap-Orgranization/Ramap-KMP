package com.peto.ramap.platform

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.peto.ramap.platform.storage.GuestBookmarkStorage
import com.peto.ramap.platform.storage.impl.DefaultGuestBookmarkStorage

fun createGuestBookmarkStorage(context: Context): GuestBookmarkStorage =
    DefaultGuestBookmarkStorage(
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("guest_bookmarks.preferences_pb")
        },
    )

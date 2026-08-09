package com.peto.ramap.platform

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

fun createMapSearchHistoryStorage(): MapSearchHistoryStorage =
    DefaultMapSearchHistoryStorage(
        PreferenceDataStoreFactory.createWithPath {
            (NSHomeDirectory() + "/Library/Application Support/Ramap/map_search_history.preferences_pb").toPath()
        },
    )

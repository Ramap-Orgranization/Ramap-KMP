package com.peto.ramap.platform

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.peto.ramap.platform.storage.DefaultSearchHistoryStorage
import com.peto.ramap.platform.storage.SearchHistoryStorage
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

fun createMapSearchHistoryStorage(): SearchHistoryStorage =
    DefaultSearchHistoryStorage(
        PreferenceDataStoreFactory.createWithPath {
            (NSHomeDirectory() + "/Library/Application Support/Ramap/map_search_history.preferences_pb").toPath()
        },
    )

package com.peto.ramap.platform

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.peto.ramap.platform.storage.history.DefaultSearchHistoryStorage
import com.peto.ramap.platform.storage.history.SearchHistoryStorage

fun createMapSearchHistoryStorage(context: Context): SearchHistoryStorage =
    DefaultSearchHistoryStorage(
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("map_search_history.preferences_pb")
        },
    )

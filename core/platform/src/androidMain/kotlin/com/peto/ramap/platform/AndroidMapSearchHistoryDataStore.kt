package com.peto.ramap.platform

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile

fun createMapSearchHistoryStorage(context: Context): MapSearchHistoryStorage =
    DefaultMapSearchHistoryStorage(
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("map_search_history.preferences_pb")
        },
    )

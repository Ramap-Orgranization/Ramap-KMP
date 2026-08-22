package com.peto.ramap.data.datasource.event

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile

fun createEventReadDataSource(context: Context): EventReadDataSource =
    DefaultEventReadDataSource(
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("event_read.preferences_pb")
        },
    )

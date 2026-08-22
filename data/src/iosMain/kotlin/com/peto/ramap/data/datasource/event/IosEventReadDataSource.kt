package com.peto.ramap.data.datasource.event

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

fun createEventReadDataSource(): EventReadDataSource =
    DefaultEventReadDataSource(
        PreferenceDataStoreFactory.createWithPath {
            (NSHomeDirectory() + "/Library/Application Support/Ramap/event_read.preferences_pb").toPath()
        },
    )

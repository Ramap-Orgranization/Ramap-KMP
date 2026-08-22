package com.peto.ramap.data.datasource.event

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class DefaultEventReadDataSource(
    private val dataStore: DataStore<Preferences>,
) : EventReadDataSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val readEventIds: StateFlow<Set<String>?> =
        dataStore.data.map { it[READ_EVENT_IDS_KEY] ?: emptySet() }.stateIn(
            scope,
            SharingStarted.Eagerly,
            null,
        )

    override suspend fun markAsRead(eventId: String) {
        if (eventId.isBlank()) return
        dataStore.edit { preferences ->
            preferences[READ_EVENT_IDS_KEY] = preferences[READ_EVENT_IDS_KEY].orEmpty() + eventId
        }
    }

    private companion object {
        val READ_EVENT_IDS_KEY = stringSetPreferencesKey("read_event_ids")
    }
}

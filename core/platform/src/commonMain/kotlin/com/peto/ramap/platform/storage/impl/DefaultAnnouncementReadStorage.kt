package com.peto.ramap.platform.storage.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.peto.ramap.platform.storage.AnnouncementReadStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DefaultAnnouncementReadStorage(
    private val dataStore: DataStore<Preferences>,
) : AnnouncementReadStorage {
    override val readIds: Flow<Set<String>> =
        dataStore.data.map { it[READ_IDS] ?: emptySet() }.distinctUntilChanged()

    override suspend fun markRead(announcementId: String) {
        if (announcementId.isBlank()) return
        dataStore.edit { it[READ_IDS] = (it[READ_IDS] ?: emptySet()) + announcementId }
    }

    private companion object {
        val READ_IDS = stringSetPreferencesKey("guest_read_announcement_ids")
    }
}

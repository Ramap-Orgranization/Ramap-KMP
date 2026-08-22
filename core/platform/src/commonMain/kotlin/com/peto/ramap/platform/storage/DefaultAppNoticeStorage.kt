package com.peto.ramap.platform.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class DefaultAppNoticeStorage(
    private val dataStore: DataStore<Preferences>,
) : AppNoticeStorage {
    override suspend fun fetchHiddenNoticeId(): String? = dataStore.data.first()[HIDDEN_NOTICE_ID_KEY]

    override suspend fun hideNotice(noticeId: String) {
        if (noticeId.isBlank()) return

        dataStore.edit { it[HIDDEN_NOTICE_ID_KEY] = noticeId }
    }

    private companion object {
        val HIDDEN_NOTICE_ID_KEY = stringPreferencesKey("hidden_app_notice_id")
    }
}

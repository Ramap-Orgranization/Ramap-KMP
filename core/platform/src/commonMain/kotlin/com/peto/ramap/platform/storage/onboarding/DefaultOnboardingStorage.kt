package com.peto.ramap.platform.storage.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

class DefaultOnboardingStorage(
    private val dataStore: DataStore<Preferences>,
) : OnboardingStorage {
    override suspend fun isPermissionGuideConfirmed(): Boolean = dataStore.data.first()[PERMISSION_GUIDE_CONFIRMED_KEY] ?: false

    override suspend fun confirmPermissionGuide() {
        dataStore.edit { it[PERMISSION_GUIDE_CONFIRMED_KEY] = true }
    }

    private companion object {
        val PERMISSION_GUIDE_CONFIRMED_KEY = booleanPreferencesKey("permission_guide_confirmed")
    }
}

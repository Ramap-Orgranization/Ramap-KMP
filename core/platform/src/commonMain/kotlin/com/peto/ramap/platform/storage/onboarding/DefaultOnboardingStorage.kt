package com.peto.ramap.platform.storage.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class DefaultOnboardingStorage(
    private val dataStore: DataStore<Preferences>,
) : OnboardingStorage {
    override suspend fun isPermissionGuideConfirmed(): Boolean =
        try {
            dataStore.data.first()[PERMISSION_GUIDE_CONFIRMED_KEY] ?: false
        } catch (exception: Exception) {
            false
        }

    override suspend fun confirmPermissionGuide() {
        try {
            dataStore.edit { it[PERMISSION_GUIDE_CONFIRMED_KEY] = true }
        } catch (exception: Exception) {
            // 안내 기록을 저장하지 못해도 현재 실행에서 앱 이용을 막지 않는다.
        }
    }

    override suspend fun savedShopCount(): Int? =
        try {
            dataStore.data.first()[SAVED_SHOP_COUNT_KEY]
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }

    override suspend fun saveShopSelection(savedShopCount: Int) {
        dataStore.edit { it[SAVED_SHOP_COUNT_KEY] = savedShopCount }
    }

    override suspend fun isOnboardingCompleted(): Boolean =
        try {
            dataStore.data.first()[ONBOARDING_COMPLETED_KEY] ?: false
        } catch (exception: Exception) {
            false
        }

    override suspend fun completeOnboarding() {
        try {
            dataStore.edit {
                it[ONBOARDING_COMPLETED_KEY] = true
                it.remove(SAVED_SHOP_COUNT_KEY)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            // 완료 기록을 저장하지 못해도 현재 실행에서 앱 이용을 막지 않는다.
        }
    }

    private companion object {
        val PERMISSION_GUIDE_CONFIRMED_KEY = booleanPreferencesKey("permission_guide_confirmed")
        val SAVED_SHOP_COUNT_KEY = intPreferencesKey("onboarding_saved_shop_count")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }
}

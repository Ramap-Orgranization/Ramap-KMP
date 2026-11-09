package com.peto.ramap.platform

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.peto.ramap.platform.storage.onboarding.DefaultOnboardingStorage
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage

fun createOnboardingStorage(context: Context): OnboardingStorage =
    DefaultOnboardingStorage(
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("onboarding.preferences_pb")
        },
    )

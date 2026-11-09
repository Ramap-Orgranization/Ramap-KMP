package com.peto.ramap.platform

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.peto.ramap.platform.storage.onboarding.DefaultOnboardingStorage
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import okio.Path.Companion.toPath
import platform.Foundation.NSHomeDirectory

fun createOnboardingStorage(): OnboardingStorage =
    DefaultOnboardingStorage(
        PreferenceDataStoreFactory.createWithPath {
            (NSHomeDirectory() + "/Library/Application Support/Ramap/onboarding.preferences_pb").toPath()
        },
    )

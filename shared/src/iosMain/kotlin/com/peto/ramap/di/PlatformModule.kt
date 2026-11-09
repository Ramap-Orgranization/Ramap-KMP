package com.peto.ramap.di

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.CrashReporter
import com.peto.ramap.analytics.FirebaseAnalyticsTracker
import com.peto.ramap.analytics.FirebaseCrashReporter
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.platform.IosAppSettingsOpener
import com.peto.ramap.platform.IosAppVersionProvider
import com.peto.ramap.platform.createAnnouncementReadStorage
import com.peto.ramap.platform.createAppNoticeStorage
import com.peto.ramap.platform.createGuestBookmarkStorage
import com.peto.ramap.platform.createMapSearchHistoryStorage
import com.peto.ramap.platform.createOnboardingStorage
import com.peto.ramap.platform.network.IosNetworkConnectivityObserver
import com.peto.ramap.platform.network.NetworkConnectivityObserver
import com.peto.ramap.platform.storage.announcement.AnnouncementReadStorage
import com.peto.ramap.platform.storage.bookmark.GuestBookmarkStorage
import com.peto.ramap.platform.storage.history.SearchHistoryStorage
import com.peto.ramap.platform.storage.notice.AppNoticeStorage
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import org.koin.dsl.module

actual val platformModule =
    module {
        single<AnnouncementReadStorage> { createAnnouncementReadStorage() }
        single<GuestBookmarkStorage> { createGuestBookmarkStorage() }
        single<AppSettingsOpener> {
            IosAppSettingsOpener()
        }
        single<AppVersionProvider> { IosAppVersionProvider() }
        single<AppNoticeStorage> { createAppNoticeStorage() }
        single<OnboardingStorage> { createOnboardingStorage() }
        single<SearchHistoryStorage> { createMapSearchHistoryStorage() }
        single<NetworkConnectivityObserver> { IosNetworkConnectivityObserver() }
        single<AnalyticsTracker> { FirebaseAnalyticsTracker() }
        single<CrashReporter> { FirebaseCrashReporter() }
    }

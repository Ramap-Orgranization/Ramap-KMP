package com.peto.ramap.data.repository.di

import com.peto.ramap.data.datasource.importation.ImportationDataSource
import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.data.datasource.place.PlaceSearchDataSource
import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.datasource.update.AppUpdatePolicyDataSource
import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import com.peto.ramap.data.repository.DefaultAppUpdateRepository
import com.peto.ramap.data.repository.DefaultBookmarkRepository
import com.peto.ramap.data.repository.DefaultHiddenShopRepository
import com.peto.ramap.data.repository.DefaultImportationRepository
import com.peto.ramap.data.repository.DefaultLoginRepository
import com.peto.ramap.data.repository.DefaultNotificationSettingsRepository
import com.peto.ramap.data.repository.DefaultPlaceSearchRepository
import com.peto.ramap.data.repository.DefaultPushRegistrationRepository
import com.peto.ramap.data.repository.DefaultRamenShopRepository
import com.peto.ramap.data.repository.DefaultShopRankingRepository
import com.peto.ramap.data.repository.DefaultShopReportRepository
import com.peto.ramap.data.repository.DefaultShopWaitingSystemRepository
import com.peto.ramap.data.repository.DefaultSubscribedShopRepository
import com.peto.ramap.data.store.DefaultShopPersonalizationStore
import com.peto.ramap.domain.repository.AppUpdateRepository
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.domain.repository.HiddenShopRepository
import com.peto.ramap.domain.repository.ImportationRepository
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.PlaceSearchRepository
import com.peto.ramap.domain.repository.PushRegistrationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopRankingRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.domain.repository.SubscribedShopRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import org.koin.dsl.module

val repositoryModule =
    module {
        single<AppUpdateRepository> {
            DefaultAppUpdateRepository(get<AppUpdatePolicyDataSource>())
        }
        single<ImportationRepository> { DefaultImportationRepository(get<ImportationDataSource>()) }
        single<LoginRepository> {
            DefaultLoginRepository(get())
        }
        single<PushRegistrationRepository> {
            DefaultPushRegistrationRepository(get())
        }
        single<RamenShopRepository> {
            DefaultRamenShopRepository(get<RamenShopDataSource>())
        }
        single<NotificationSettingsRepository> { DefaultNotificationSettingsRepository(get()) }
        single<PlaceSearchRepository> { DefaultPlaceSearchRepository(get<PlaceSearchDataSource>()) }
        single<ShopWaitingSystemRepository> {
            DefaultShopWaitingSystemRepository(get<ShopWaitingSystemDataSource>())
        }
        single<BookmarkRepository> { DefaultBookmarkRepository(get<BookmarkShopDataSource>()) }
        single<HiddenShopRepository> { DefaultHiddenShopRepository(get<HiddenShopDataSource>()) }
        single<SubscribedShopRepository> { DefaultSubscribedShopRepository(get()) }
        single<ShopPersonalizationStore> {
            DefaultShopPersonalizationStore(get(), get(), get())
        }
        single<ShopReportRepository> {
            DefaultShopReportRepository(get<ShopReportDataSource>())
        }
        single<ShopRankingRepository> {
            DefaultShopRankingRepository(get())
        }
    }

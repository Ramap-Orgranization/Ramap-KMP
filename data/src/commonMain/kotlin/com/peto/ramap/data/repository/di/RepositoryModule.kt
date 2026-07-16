package com.peto.ramap.data.repository.di

import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import com.peto.ramap.data.repository.DefaultLoginRepository
import com.peto.ramap.data.repository.DefaultNotificationSettingsRepository
import com.peto.ramap.data.repository.DefaultPersonalizationRepository
import com.peto.ramap.data.repository.DefaultRamenShopRepository
import com.peto.ramap.data.repository.DefaultShopReportRepository
import com.peto.ramap.data.repository.DefaultShopWaitingSystemRepository
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import org.koin.dsl.module

val repositoryModule =
    module {
        single<LoginRepository> {
            DefaultLoginRepository(get())
        }
        single<RamenShopRepository> {
            DefaultRamenShopRepository(get<RamenShopDataSource>())
        }
        single<NotificationSettingsRepository> { DefaultNotificationSettingsRepository(get()) }
        single<ShopWaitingSystemRepository> {
            DefaultShopWaitingSystemRepository(get<ShopWaitingSystemDataSource>())
        }
        single<PersonalizationRepository> {
            DefaultPersonalizationRepository(
                get<BookmarkShopDataSource>(),
                get<HiddenShopDataSource>(),
            )
        }
        single<ShopReportRepository> {
            DefaultShopReportRepository(get<ShopReportDataSource>())
        }
    }

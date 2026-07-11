package com.peto.ramap.data.repository.di

import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import com.peto.ramap.data.repository.DefaultBookmarkShopRepository
import com.peto.ramap.data.repository.DefaultHiddenShopRepository
import com.peto.ramap.data.repository.DefaultLoginRepository
import com.peto.ramap.data.repository.DefaultPersonalizationRepository
import com.peto.ramap.data.repository.DefaultRamenShopRepository
import com.peto.ramap.data.repository.DefaultShopReportRepository
import com.peto.ramap.data.repository.DefaultShopWaitingSystemRepository
import com.peto.ramap.domain.repository.BookmarkShopRepository
import com.peto.ramap.domain.repository.HiddenShopRepository
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import org.koin.dsl.module

val repositoryModule =
    module {
        single<LoginRepository> {
            DefaultLoginRepository(get(), get())
        }
        single<RamenShopRepository> {
            DefaultRamenShopRepository(get<RamenShopDataSource>())
        }
        single<ShopWaitingSystemRepository> {
            DefaultShopWaitingSystemRepository(get<ShopWaitingSystemDataSource>())
        }
        single<BookmarkShopRepository> {
            DefaultBookmarkShopRepository(get<BookmarkShopDataSource>())
        }
        single<HiddenShopRepository> {
            DefaultHiddenShopRepository(get<HiddenShopDataSource>())
        }
        single<PersonalizationRepository> {
            DefaultPersonalizationRepository(get(), get())
        }
        single<ShopReportRepository> {
            DefaultShopReportRepository(get<ShopReportDataSource>())
        }
    }

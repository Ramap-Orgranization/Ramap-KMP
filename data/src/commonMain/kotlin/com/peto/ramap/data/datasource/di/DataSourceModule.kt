package com.peto.ramap.data.datasource.di

import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.data.datasource.personalization.RemoteBookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.RemoteHiddenShopDataSource
import com.peto.ramap.data.datasource.report.RemoteShopReportDataSource
import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.datasource.shop.RemoteRamenShopDataSource
import com.peto.ramap.data.datasource.waiting.RemoteShopWaitingSystemDataSource
import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

val dataSourceModule =
    module {
        single<RamenShopDataSource> {
            RemoteRamenShopDataSource(get<SupabaseClient>())
        }
        single<ShopWaitingSystemDataSource> {
            RemoteShopWaitingSystemDataSource(get<SupabaseClient>())
        }
        single<BookmarkShopDataSource> {
            RemoteBookmarkShopDataSource(get<SupabaseClient>())
        }
        single<HiddenShopDataSource> {
            RemoteHiddenShopDataSource(get<SupabaseClient>())
        }
        single<ShopReportDataSource> {
            RemoteShopReportDataSource(get<SupabaseClient>())
        }
    }

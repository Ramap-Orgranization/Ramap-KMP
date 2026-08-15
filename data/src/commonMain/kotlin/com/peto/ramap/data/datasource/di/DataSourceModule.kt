package com.peto.ramap.data.datasource.di

import com.peto.ramap.data.datasource.geocoder.SupabaseReverseGeocoder
import com.peto.ramap.data.datasource.importation.ImportationDataSource
import com.peto.ramap.data.datasource.importation.KakaoImportationDataSource
import com.peto.ramap.data.datasource.importation.NaverImportationDataSource
import com.peto.ramap.data.datasource.importation.RemoteImportationDataSource
import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.data.datasource.personalization.RemoteBookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.RemoteHiddenShopDataSource
import com.peto.ramap.data.datasource.place.PlaceSearchDataSource
import com.peto.ramap.data.datasource.place.RemotePlaceSearchDataSource
import com.peto.ramap.data.datasource.ranking.RemoteShopRankingDataSource
import com.peto.ramap.data.datasource.ranking.ShopRankingDataSource
import com.peto.ramap.data.datasource.report.RemoteShopReportDataSource
import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.datasource.shop.RemoteRamenShopDataSource
import com.peto.ramap.data.datasource.waiting.RemoteShopWaitingSystemDataSource
import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import com.peto.ramap.domain.repository.ReverseGeocoder
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

val dataSourceModule =
    module {
        single { NaverImportationDataSource(get(), get<HttpClient>()) }
        single { KakaoImportationDataSource(get()) }
        single<ImportationDataSource> { RemoteImportationDataSource(get(), get()) }
        single<PlaceSearchDataSource> {
            RemotePlaceSearchDataSource(get<SupabaseClient>())
        }
        single<RamenShopDataSource> {
            RemoteRamenShopDataSource(get<SupabaseClient>())
        }
        single<ShopRankingDataSource> {
            RemoteShopRankingDataSource(get<SupabaseClient>())
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
        single<ReverseGeocoder> { SupabaseReverseGeocoder(get()) }
    }

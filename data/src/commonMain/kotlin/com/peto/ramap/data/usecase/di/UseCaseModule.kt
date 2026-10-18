package com.peto.ramap.data.usecase.di

import com.peto.ramap.data.usecase.DefaultFetchShopOverviewUseCase
import com.peto.ramap.domain.model.review.ShopReviewCache
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.usecase.FeatchShopReviewsUseCase
import com.peto.ramap.domain.usecase.FetchShopOverviewUseCase
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule =
    module {
        singleOf(::ReviewChangeReducer)
        factory<FetchShopOverviewUseCase> {
            DefaultFetchShopOverviewUseCase(
                ramenShopRepository = get(),
                operatingNoticeRepository = get(),
            )
        }
        factoryOf(::ShopReviewCache)
        factoryOf(::FeatchShopReviewsUseCase)
        factoryOf(::SearchRamenShopsUseCase)
        factoryOf(::SignInUseCase)
    }

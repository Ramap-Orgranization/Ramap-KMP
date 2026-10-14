package com.peto.ramap.data.usecase.di

import com.peto.ramap.data.usecase.DefaultFetchShopDetailUseCase
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.LoadShopReviewsUseCase
import com.peto.ramap.domain.usecase.LoginSessionUseCase
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule =
    module {
        singleOf(::ReviewChangeReducer)
        factory<FetchShopDetailUseCase> {
            DefaultFetchShopDetailUseCase(
                ramenShopRepository = get(),
                operatingNoticeRepository = get(),
            )
        }
        factoryOf(::LoadShopReviewsUseCase)
        factoryOf(::SearchRamenShopsUseCase)
        factoryOf(::SignInUseCase)
        factoryOf(::LoginSessionUseCase)
    }

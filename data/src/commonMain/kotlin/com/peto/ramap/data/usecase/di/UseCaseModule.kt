package com.peto.ramap.data.usecase.di

import com.peto.ramap.data.usecase.DefaultFetchShopDetailUseCase
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import org.koin.dsl.module

val useCaseModule =
    module {
        factory<FetchShopDetailUseCase> {
            DefaultFetchShopDetailUseCase(get())
        }
    }

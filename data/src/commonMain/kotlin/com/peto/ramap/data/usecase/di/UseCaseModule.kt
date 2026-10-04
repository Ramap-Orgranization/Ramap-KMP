package com.peto.ramap.data.usecase.di

import com.peto.ramap.data.usecase.DefaultFetchShopDetailUseCase
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.LoginSessionUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule =
    module {
        factory<FetchShopDetailUseCase> {
            DefaultFetchShopDetailUseCase(get(), get(), get())
        }
        factoryOf(::SignInUseCase)
        factoryOf(::LoginSessionUseCase)
    }

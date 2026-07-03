package com.peto.ramap.data.repository.di

import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import com.peto.ramap.data.repository.DefaultLoginRepository
import com.peto.ramap.data.repository.DefaultRamenShopRepository
import com.peto.ramap.data.repository.DefaultShopWaitingSystemRepository
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
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
        single<ShopWaitingSystemRepository> {
            DefaultShopWaitingSystemRepository(get<ShopWaitingSystemDataSource>())
        }
    }

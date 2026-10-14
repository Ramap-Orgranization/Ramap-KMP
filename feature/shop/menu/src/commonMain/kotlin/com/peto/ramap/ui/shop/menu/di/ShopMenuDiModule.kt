package com.peto.ramap.ui.shop.menu.di

import com.peto.ramap.ui.shop.menu.ShopMenuViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val shopMenuModule = module { viewModelOf(::ShopMenuViewModel) }

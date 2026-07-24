package com.peto.ramap.ui.main.ranking.di

import com.peto.ramap.ui.main.ranking.RankingViewModel
import org.koin.dsl.module

val rankingModule =
    module {
        factory { RankingViewModel(get(), get(), get(), get()) }
    }

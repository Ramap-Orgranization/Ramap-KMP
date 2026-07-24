package com.peto.ramap.ui.main.ranking.di

import com.peto.ramap.ui.main.ranking.RankingAnalytics
import com.peto.ramap.ui.main.ranking.RankingViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val rankingModule =
    module {
        viewModelOf(::RankingViewModel)
        singleOf(::RankingAnalytics)
    }

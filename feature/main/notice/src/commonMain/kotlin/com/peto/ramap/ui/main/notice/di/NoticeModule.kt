package com.peto.ramap.ui.main.notice.di

import com.peto.ramap.ui.main.notice.OperatingNoticeViewModel
import com.peto.ramap.ui.main.notice.log.OperatingNoticeAnalytics
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val noticeModule =
    module {
        singleOf(::OperatingNoticeAnalytics)
        viewModelOf(::OperatingNoticeViewModel)
    }

package com.peto.ramap.ui.main.notice.di

import com.peto.ramap.ui.main.notice.OperatingNoticeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val noticeModule =
    module {
        viewModelOf(::OperatingNoticeViewModel)
    }

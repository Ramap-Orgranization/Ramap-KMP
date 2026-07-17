package com.peto.ramap.designsystem.di

import com.peto.ramap.designsystem.toast.ToastManager
import org.koin.dsl.module

val designSystemModule =
    module {
        single { ToastManager() }
    }

package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.ui.base.Intent

sealed interface MyTabIntent : Intent {
    data object Refresh : MyTabIntent
}

package com.peto.ramap.ui.hidden.contract

import com.peto.ramap.core.base.Intent

sealed interface HiddenShopListIntent : Intent

data object OnHiddenShopListRetried : HiddenShopListIntent

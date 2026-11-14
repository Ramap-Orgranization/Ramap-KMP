package com.peto.ramap.ui.main.newshops.contract

import com.peto.ramap.domain.model.shop.ShopRegistrationMonth
import com.peto.ramap.ui.base.Intent

internal sealed interface NewShopsIntent : Intent {
    data class OnShown(
        val month: ShopRegistrationMonth,
    ) : NewShopsIntent

    data class OnRefreshRequested(
        val month: ShopRegistrationMonth,
    ) : NewShopsIntent
}

package com.peto.ramap.ui.main.newshops.contract

import com.peto.ramap.domain.model.shop.ShopOpeningMonth
import com.peto.ramap.ui.base.Intent

internal sealed interface NewShopsIntent : Intent {
    data class OnShown(
        val month: ShopOpeningMonth,
    ) : NewShopsIntent

    data class OnRefreshRequested(
        val month: ShopOpeningMonth,
    ) : NewShopsIntent
}

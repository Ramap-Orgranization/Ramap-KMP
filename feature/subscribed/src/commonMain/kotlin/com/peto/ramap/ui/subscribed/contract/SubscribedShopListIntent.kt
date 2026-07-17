package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.ui.base.Intent
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget

sealed interface SubscribedShopListIntent : Intent {
    data object OnRetry : SubscribedShopListIntent

    data class OnRemovalRequested(
        val target: SubscribedRemovalTarget,
    ) : SubscribedShopListIntent

    data object OnRemovalDismissed : SubscribedShopListIntent

    data object OnRemovalConfirmed : SubscribedShopListIntent
}

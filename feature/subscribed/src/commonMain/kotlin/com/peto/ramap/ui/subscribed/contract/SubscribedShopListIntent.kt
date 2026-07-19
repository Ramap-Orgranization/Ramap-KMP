package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.ui.base.Intent
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget

sealed interface SubscribedShopListIntent : Intent {
    data class OnRemovalConfirmed(
        val target: SubscribedRemovalTarget,
    ) : SubscribedShopListIntent
}

package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.ui.base.Intent
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget

sealed interface SubscribedShopsIntent : Intent {
    data object OnRetry : SubscribedShopsIntent

    data class OnRemovalConfirmed(
        val target: SubscribedRemovalTarget,
    ) : SubscribedShopsIntent
}

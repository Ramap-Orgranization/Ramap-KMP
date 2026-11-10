package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.ui.base.Intent

sealed interface SubscribedShopsIntent : Intent {
    data object OnRetry : SubscribedShopsIntent

    data class OnEventRemovalConfirmed(
        val eventId: String,
    ) : SubscribedShopsIntent
}

package com.peto.ramap.ui.bookmark.importation.contract

import com.peto.ramap.ui.base.Intent

sealed interface ImportationIntent : Intent {
    data class UrlChanged(
        val url: String,
    ) : ImportationIntent

    data class CandidateRemoved(
        val shopId: String,
    ) : ImportationIntent

    data object Analyze : ImportationIntent

    data object Retry : ImportationIntent

    data object Confirm : ImportationIntent

    data object Reset : ImportationIntent
}

package com.peto.ramap.domain.store

import com.peto.ramap.domain.model.review.change.ReviewChangeJournal
import kotlinx.coroutines.flow.StateFlow

interface ReviewChangeStore {
    val journal: StateFlow<ReviewChangeJournal>
}

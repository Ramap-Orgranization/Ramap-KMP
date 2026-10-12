package com.peto.ramap.data

import com.peto.ramap.data.store.DefaultReviewChangeStore
import kotlinx.coroutines.flow.flowOf

internal fun testReviewChanges() = DefaultReviewChangeStore(flowOf(null), { null })

package com.peto.ramap.data.store

import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeJournal
import com.peto.ramap.domain.model.review.change.RevisionedReviewChange
import com.peto.ramap.domain.store.ReviewChangeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class DefaultReviewChangeStore(
    sessionUserIds: Flow<String?>,
    private val currentUserId: () -> String?,
) : ReviewChangeStore {
    private val state = MutableStateFlow(ReviewChangeJournal())
    override val journal = state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        synchronizeSession()
        scope.launch {
            sessionUserIds.distinctUntilChanged().collect {
                synchronizeSession()
            }
        }
    }

    fun synchronizeSession(): Long {
        state.update { current ->
            val userId = currentUserId()
            if (current.userId == userId) current else ReviewChangeJournal(epoch = current.epoch + 1, userId = userId)
        }
        return state.value.epoch
    }

    fun publish(
        change: ReviewChange,
        epoch: Long,
    ) {
        synchronizeSession()
        state.update { current ->
            if (current.epoch != epoch) return@update current
            val revision = current.revision + 1
            current.copy(
                revision = revision,
                changes = (current.changes + RevisionedReviewChange(revision, change)).takeLast(JOURNAL_LIMIT),
            )
        }
    }

    private companion object {
        const val JOURNAL_LIMIT = 128
    }
}

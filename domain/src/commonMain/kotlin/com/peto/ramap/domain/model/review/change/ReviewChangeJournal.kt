package com.peto.ramap.domain.model.review.change

data class ReviewChangeJournal(
    val epoch: Long = 0,
    val userId: String? = null,
    val revision: Long = 0,
    val changes: List<RevisionedReviewChange> = emptyList(),
) {
    fun hasGapAfter(revision: Long): Boolean = changes.firstOrNull()?.revision?.let { revision < it - 1 } == true

    fun changesAfter(revision: Long): List<RevisionedReviewChange> = changes.filter { it.revision > revision }

    fun requiresShopReloadAfter(
        revision: Long,
        shopId: String,
    ): Boolean =
        hasGapAfter(revision) ||
            changesAfter(revision).any {
                (it.change as? ReviewChange.Written)?.shopId == shopId || it.change is ReviewChange.AccessChanged
            }
}

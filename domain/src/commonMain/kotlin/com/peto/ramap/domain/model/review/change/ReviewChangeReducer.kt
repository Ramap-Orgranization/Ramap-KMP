package com.peto.ramap.domain.model.review.change

import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.Review
import kotlin.time.Clock
import kotlin.time.Instant

class ReviewChangeReducer {
    private val reviewOrder = compareByDescending<Review> { it.createdAt }.thenByDescending { it.id }

    fun patch(
        rows: List<Review>,
        change: ReviewChange,
        hasMore: Boolean,
        accepts: (ReviewChange.Written, Review) -> Boolean,
    ): List<Review> =
        when (change) {
            is ReviewChange.Liked ->
                updateLikes(
                    rows = rows,
                    change = change,
                )
            is ReviewChange.Hydrated ->
                hydrateReviews(
                    rows = rows,
                    hydrated = change.snapshot.review,
                )
            is ReviewChange.AccessChanged -> rows.filterNot { it.author.userId == change.userId }
            is ReviewChange.Written ->
                patchWritten(
                    rows = rows,
                    change = change,
                    hasMore = hasMore,
                    accepts = accepts,
                )
        }

    private fun updateLikes(
        rows: List<Review>,
        change: ReviewChange.Liked,
    ): List<Review> =
        rows.map { review ->
            if (review.id == change.reviewId) {
                review.copy(
                    likeCount = change.result.likeCount,
                    isLiked = change.result.isLiked,
                )
            } else {
                review
            }
        }

    private fun hydrateReviews(
        rows: List<Review>,
        hydrated: Review,
    ): List<Review> =
        rows.map {
            hydrateReview(
                current = it,
                hydrated = hydrated,
            )
        }

    private fun hydrateReview(
        current: Review,
        hydrated: Review,
    ): Review {
        if (current.id != hydrated.id) return current
        if (current.images.map { it.reference } != hydrated.images.map { it.reference }) return current

        val retained =
            retainMedia(
                next = hydrated,
                previous = current,
            )
        return current.copy(
            images = retained.images,
            imageUrls = retained.imageUrls,
            author =
                hydrateAuthor(
                    current = current.author,
                    hydrated = retained.author,
                ),
        )
    }

    private fun hydrateAuthor(
        current: ReviewAuthor,
        hydrated: ReviewAuthor,
    ): ReviewAuthor {
        if (current.avatarReference != hydrated.avatarReference) return current
        return current.copy(
            avatarUrl = hydrated.avatarUrl,
            avatarExpiresAt = hydrated.avatarExpiresAt,
        )
    }

    private fun patchWritten(
        rows: List<Review>,
        change: ReviewChange.Written,
        hasMore: Boolean,
        accepts: (ReviewChange.Written, Review) -> Boolean,
    ): List<Review> {
        val previous = rows.firstOrNull { it.id == change.reviewId }
        val next = change.after?.review?.takeIf { accepts(change, it) }
        val result =
            updateRelatedReviews(
                rows = rows,
                change = change,
            )
        if (next != null &&
            shouldInsertReview(
                next = next,
                previous = previous,
                boundary = rows.lastOrNull(),
                hasMore = hasMore,
            )
        ) {
            val retained =
                retainMedia(
                    next = next,
                    previous = previous,
                )
            result += retained.copy(author = retained.author.copy(reviewCount = change.counts.publishedCount))
        }
        return result.sortedWith(reviewOrder)
    }

    private fun updateRelatedReviews(
        rows: List<Review>,
        change: ReviewChange.Written,
    ): MutableList<Review> {
        val visitDelta = findVisitDelta(change)
        val anchor = change.before?.review ?: change.after?.review
        val result = mutableListOf<Review>()
        for (review in rows) {
            if (review.id == change.reviewId) continue
            result +=
                updateAuthorSummary(
                    review = review,
                    change = change,
                    anchor = anchor,
                    visitDelta = visitDelta,
                )
        }
        return result
    }

    private fun findVisitDelta(change: ReviewChange.Written): Int {
        val after = if (change.after?.review?.let(::isPublicPublished) == true) 1 else 0
        val before = if (change.before?.review?.let(::isPublicPublished) == true) 1 else 0
        return after - before
    }

    private fun updateAuthorSummary(
        review: Review,
        change: ReviewChange.Written,
        anchor: Review?,
        visitDelta: Int,
    ): Review {
        if (review.author.userId != change.userId) return review
        val isLaterVisit = anchor != null && review.shopId == change.shopId && reviewOrder.compare(review, anchor) < 0
        return review.copy(
            author = review.author.copy(reviewCount = change.counts.publishedCount),
            visitNumber = (review.visitNumber + if (isLaterVisit) visitDelta else 0).coerceAtLeast(0),
        )
    }

    private fun shouldInsertReview(
        next: Review,
        previous: Review?,
        boundary: Review?,
        hasMore: Boolean,
    ): Boolean {
        if (previous != null || !hasMore) return true
        return boundary != null && reviewOrder.compare(next, boundary) <= 0
    }

    fun retainMedia(
        next: Review,
        previous: Review?,
    ): Review {
        if (previous == null) return next
        val now = Clock.System.now()
        val images =
            retainImages(
                next = next.images,
                previous = previous.images,
                now = now,
            )
        return next.copy(
            images = images,
            imageUrls = if (images.isNotEmpty()) images.mapNotNull { it.url } else next.imageUrls,
            author =
                retainAvatar(
                    next = next.author,
                    previous = previous.author,
                    now = now,
                ),
        )
    }

    private fun retainImages(
        next: List<MyReviewPhoto>,
        previous: List<MyReviewPhoto>,
        now: Instant,
    ): List<MyReviewPhoto> {
        val previousImages = previous.associateBy { it.reference }
        return next.map { image ->
            val retained = previousImages[image.reference]
            if (retained?.url != null && (retained.expiresAt == null || retained.expiresAt > now)) retained else image
        }
    }

    private fun retainAvatar(
        next: ReviewAuthor,
        previous: ReviewAuthor,
        now: Instant,
    ): ReviewAuthor {
        if (previous.avatarUrl == null || previous.avatarReference != next.avatarReference) return next
        if (previous.avatarExpiresAt != null && previous.avatarExpiresAt <= now) return next
        return next.copy(
            avatarUrl = previous.avatarUrl,
            avatarExpiresAt = previous.avatarExpiresAt,
        )
    }

    fun isShopVisible(
        change: ReviewChange.Written,
        review: Review,
        viewerId: String?,
    ): Boolean = review.author.userId == viewerId || isPublicPublished(review) && change.counts.profileIsPublic

    fun isPublicPublished(review: Review): Boolean = review.isPublic && review.moderationStatus == ReviewModerationStatus.PUBLISHED

    fun updateProfileRows(
        rows: List<ProfileReview>,
        change: ReviewChange,
        hasMore: Boolean,
        accepts: (ReviewChange.Written, Review) -> Boolean,
    ): List<ProfileReview> {
        val previous = rows.associateBy { it.review.id }
        val snapshot = findSnapshot(change)
        val patched =
            patch(
                rows = rows.map { it.review },
                change = change,
                hasMore = hasMore,
                accepts = accepts,
            )
        return patched.map { review ->
            ProfileReview(
                review = review,
                shop = snapshot?.takeIf { it.review.id == review.id }?.shop ?: previous[review.id]?.shop,
            )
        }
    }

    fun updateOwnerRows(
        rows: List<MyReview>,
        change: ReviewChange,
        filter: MyReviewVisibility,
        hasMore: Boolean,
    ): List<MyReview> {
        val previous = rows.associateBy { it.id }
        val canonical =
            rows.map {
                createOwnerReview(
                    row = it,
                    change = change,
                )
            }
        val patched =
            patch(
                rows = canonical,
                change = change,
                hasMore = hasMore,
                accepts = { _, review ->
                    isOwnerReviewVisible(
                        review = review,
                        filter = filter,
                    )
                },
            )
        val snapshot = findSnapshot(change)
        return patched.map { review ->
            createOwnerRow(
                review = review,
                previous = previous[review.id],
                snapshot = snapshot?.takeIf { it.review.id == review.id },
            )
        }
    }

    private fun createOwnerReview(
        row: MyReview,
        change: ReviewChange,
    ): Review {
        val review = row.toReview()
        if (change !is ReviewChange.Written || review.author.userId.isNotBlank()) return review
        return review.copy(author = review.author.copy(userId = change.userId))
    }

    private fun isOwnerReviewVisible(
        review: Review,
        filter: MyReviewVisibility,
    ): Boolean =
        when (filter) {
            MyReviewVisibility.ALL -> true
            MyReviewVisibility.PUBLIC -> review.isPublic
            MyReviewVisibility.PRIVATE -> !review.isPublic
        }

    private fun findSnapshot(change: ReviewChange): ReviewSnapshot? =
        when (change) {
            is ReviewChange.Written -> change.after
            is ReviewChange.Hydrated -> change.snapshot
            else -> null
        }

    private fun createOwnerRow(
        review: Review,
        previous: MyReview?,
        snapshot: ReviewSnapshot?,
    ): MyReview =
        MyReview(
            id = review.id,
            shopId = review.shopId,
            shopName = snapshot?.shopName ?: previous?.shopName.orEmpty(),
            body = review.body,
            createdAt = review.createdAt,
            images = review.images,
            isPublic = review.isPublic,
            moderationStatus = review.moderationStatus,
            moderationNote = snapshot?.moderationNote ?: previous?.moderationNote,
            authorAvatarUrl = review.author.avatarUrl,
            visitNumber = review.visitNumber,
            authorReviewCount = review.author.reviewCount,
            shop = snapshot?.shop ?: previous?.shop,
            author = review.author,
        )
}

package com.peto.ramap.domain.model.review.change

import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.Review
import kotlin.time.Clock

object ReviewChangeReducer {
    private val order = compareByDescending<Review> { it.createdAt }.thenByDescending { it.id }

    fun patch(
        rows: List<Review>,
        change: ReviewChange,
        hasMore: Boolean,
        accepts: (ReviewChange.Written, Review) -> Boolean,
    ): List<Review> =
        when (change) {
            is ReviewChange.Liked -> rows.map { if (it.id == change.reviewId) it.copy(likeCount = change.result.likeCount, isLiked = change.result.isLiked) else it }
            is ReviewChange.Hydrated ->
                rows.map { row ->
                    if (row.id != change.snapshot.review.id ||
                        row.images.map { it.reference } !=
                        change.snapshot.review.images
                            .map { it.reference }
                    ) {
                        row
                    } else {
                        mergeImages(change.snapshot.review, row).let { hydrated -> row.copy(images = hydrated.images, imageUrls = hydrated.imageUrls, author = if (row.author.avatarReference == hydrated.author.avatarReference) row.author.copy(avatarUrl = hydrated.author.avatarUrl, avatarExpiresAt = hydrated.author.avatarExpiresAt) else row.author) }
                    }
                }
            is ReviewChange.AccessChanged -> rows.filterNot { it.author.userId == change.userId }
            is ReviewChange.Written -> patchWritten(rows, change, hasMore, accepts)
        }

    private fun patchWritten(
        rows: List<Review>,
        change: ReviewChange.Written,
        hasMore: Boolean,
        accepts: (ReviewChange.Written, Review) -> Boolean,
    ): List<Review> {
        val previous = rows.firstOrNull { it.id == change.reviewId }
        val boundary = rows.lastOrNull()
        val next = change.after?.review?.takeIf { accepts(change, it) }
        val visitDelta =
            (if (change.after?.review?.let(::publicPublished) == true) 1 else 0) -
                (if (change.before?.review?.let(::publicPublished) == true) 1 else 0)
        val anchor = change.before?.review ?: change.after?.review
        val result =
            rows
                .filterNot { it.id == change.reviewId }
                .map { review ->
                    if (review.author.userId != change.userId) return@map review
                    val isLaterVisit = anchor != null && review.shopId == change.shopId && order.compare(review, anchor) < 0
                    review.copy(
                        author = review.author.copy(reviewCount = change.counts.publishedCount),
                        visitNumber = (review.visitNumber + if (isLaterVisit) visitDelta else 0).coerceAtLeast(0),
                    )
                }.toMutableList()
        if (next != null && (previous != null || !hasMore || boundary != null && order.compare(next, boundary) <= 0)) {
            result += mergeImages(next, previous).copy(author = next.author.copy(reviewCount = change.counts.publishedCount))
        }
        return result.sortedWith(order)
    }

    fun mergeImages(
        next: Review,
        previous: Review?,
    ): Review {
        if (previous == null) return next
        val now = Clock.System.now()
        val previousImages = previous.images.associateBy { it.reference }
        val images =
            next.images.map { image ->
                val retained = previousImages[image.reference]
                if (retained?.url != null && (retained.expiresAt == null || retained.expiresAt > now)) retained else image
            }
        val author = next.author
        val previousAuthor = previous.author
        val retainedAvatar =
            previousAuthor.avatarUrl != null &&
                previousAuthor.avatarReference == author.avatarReference &&
                (previousAuthor.avatarExpiresAt == null || previousAuthor.avatarExpiresAt > now)
        return next.copy(
            images = images,
            imageUrls = if (images.isNotEmpty()) images.mapNotNull { it.url } else next.imageUrls,
            author = if (retainedAvatar) author.copy(avatarUrl = previousAuthor.avatarUrl, avatarExpiresAt = previousAuthor.avatarExpiresAt) else author,
        )
    }

    fun shopVisible(
        change: ReviewChange.Written,
        review: Review,
        viewerId: String?,
    ): Boolean = review.author.userId == viewerId || publicPublished(review) && change.counts.profileIsPublic

    fun publicPublished(review: Review): Boolean = review.isPublic && review.moderationStatus == ReviewModerationStatus.PUBLISHED

    fun ownerRows(
        rows: List<MyReview>,
        change: ReviewChange,
        filter: MyReviewVisibility,
        hasMore: Boolean,
    ): List<MyReview> {
        val metadata = rows.associateBy { it.id }
        val canonical =
            rows.map { row ->
                Review(
                    id = row.id,
                    shopId = row.shopId,
                    body = row.body,
                    createdAt = row.createdAt,
                    images = row.images,
                    imageUrls = row.images.mapNotNull { it.url },
                    author =
                        row.author ?: com.peto.ramap.domain.model.community
                            .ReviewAuthor("", "", row.authorAvatarUrl, row.authorReviewCount),
                    isPublic = row.isPublic,
                    moderationStatus = row.moderationStatus,
                    visitNumber = row.visitNumber,
                ).let { review ->
                    if (change is ReviewChange.Written && review.author.userId.isBlank()) review.copy(author = review.author.copy(userId = change.userId)) else review
                }
            }
        val patched =
            patch(canonical, change, hasMore) { _, review ->
                when (filter) {
                    MyReviewVisibility.ALL -> true
                    MyReviewVisibility.PUBLIC -> review.isPublic
                    MyReviewVisibility.PRIVATE -> !review.isPublic
                }
            }
        val snapshot =
            when (change) {
                is ReviewChange.Written -> change.after
                is ReviewChange.Hydrated -> change.snapshot
                else -> null
            }
        return patched.map { review ->
            val old = metadata[review.id]
            val matching = snapshot?.takeIf { it.review.id == review.id }
            MyReview(
                id = review.id,
                shopId = review.shopId,
                shopName = matching?.shopName ?: old?.shopName.orEmpty(),
                body = review.body,
                createdAt = review.createdAt,
                images = review.images,
                isPublic = review.isPublic,
                moderationStatus = review.moderationStatus,
                moderationNote = matching?.moderationNote ?: old?.moderationNote,
                authorAvatarUrl = review.author.avatarUrl,
                visitNumber = review.visitNumber,
                authorReviewCount = review.author.reviewCount,
                shop = matching?.shop ?: old?.shop,
                author = review.author,
            )
        }
    }
}

package com.peto.ramap.ui.main.map.archive.contract

import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto

data class ArchiveMapPhotoState(
    val resolved: Map<String, MyReviewPhoto> = emptyMap(),
    val failedMarkerSignatures: Set<String> = emptySet(),
    val failedReviewDownloads: Set<String> = emptySet(),
    val refreshingReviewDownloads: Set<String> = emptySet(),
    val refreshedMarkers: Set<String> = emptySet(),
    val refreshedReviews: Set<String> = emptySet(),
    val reviewRetryKeys: Map<String, Int> = emptyMap(),
    val hasReviewSigningError: Boolean = false,
) {
    fun resolve(photo: MyReviewPhoto): MyReviewPhoto = resolved[photo.reference] ?: photo

    fun resolve(row: MyReview): MyReview {
        val images = row.review.images.map(::resolve)
        return row.copy(review = row.review.copy(images = images, imageUrls = images.mapNotNull { it.url }))
    }

    fun withResolvedPhotos(photos: Map<String, MyReviewPhoto>): ArchiveMapPhotoState {
        val available = photos.filterValues { it.url != null }
        return copy(
            resolved = resolved + available,
            failedMarkerSignatures = failedMarkerSignatures - available.keys,
        )
    }

    fun withMarkerPhotos(
        photos: Map<String, MyReviewPhoto>,
        requestedReferences: Set<String>,
    ): ArchiveMapPhotoState {
        val next = withResolvedPhotos(photos)
        return next.copy(
            failedMarkerSignatures =
                (next.failedMarkerSignatures - requestedReferences) +
                    requestedReferences.filter { next.resolved[it]?.url == null },
        )
    }

    fun withReviewPhotos(
        photos: Map<String, MyReviewPhoto>,
        requestedReferences: Set<String>,
    ): ArchiveMapPhotoState {
        val available = photos.filterValues { it.url != null }
        val next = withResolvedPhotos(available)
        val refreshes = refreshingReviewDownloads.intersect(requestedReferences)
        val retries = available.keys.intersect(failedReviewDownloads + refreshes)
        return next.copy(
            refreshingReviewDownloads = refreshingReviewDownloads - requestedReferences,
            failedReviewDownloads = failedReviewDownloads + (refreshes - available.keys),
            reviewRetryKeys = reviewRetryKeys + retries.associateWith { (reviewRetryKeys[it] ?: 0) + 1 },
            hasReviewSigningError = requestedReferences.any { next.resolved[it]?.url == null },
        )
    }

    fun withReviewSigningFailure(requestedReferences: Set<String>): ArchiveMapPhotoState =
        copy(
            refreshingReviewDownloads = refreshingReviewDownloads - requestedReferences,
            failedReviewDownloads = failedReviewDownloads + refreshingReviewDownloads.intersect(requestedReferences),
            hasReviewSigningError = true,
        )

    fun retainReviews(references: Set<String>): ArchiveMapPhotoState =
        copy(
            failedReviewDownloads = failedReviewDownloads.intersect(references),
            refreshingReviewDownloads = refreshingReviewDownloads.intersect(references),
            reviewRetryKeys = reviewRetryKeys.filterKeys { it in references },
            hasReviewSigningError = hasReviewSigningError && references.isNotEmpty(),
        )

    fun clearSelection(): ArchiveMapPhotoState =
        copy(
            failedReviewDownloads = emptySet(),
            refreshingReviewDownloads = emptySet(),
            reviewRetryKeys = emptyMap(),
            hasReviewSigningError = false,
        )

    fun clearRefreshes(): ArchiveMapPhotoState = copy(refreshedMarkers = emptySet(), refreshedReviews = emptySet())
}

package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.datasource.image.ImageUrlAccessPolicy
import com.peto.ramap.data.datasource.image.ImageUrlAccessSnapshot
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.store.ReviewChangeStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

internal class ReviewImageUrlAccessPolicy(
    private val client: SupabaseClient,
    private val changes: Lazy<ReviewChangeStore>,
) : ImageUrlAccessPolicy {
    override fun snapshot(): ImageUrlAccessSnapshot {
        val journal = changes.value.journal.value
        return ImageUrlAccessSnapshot(client.auth.currentUserOrNull()?.id, journal.epoch, journal.revision)
    }

    override fun invalidatedImages(
        snapshot: ImageUrlAccessSnapshot,
        images: Collection<SignedImageRequest>,
    ): Set<SignedImageRequest> {
        val journal = changes.value.journal.value
        if (client.auth.currentUserOrNull()?.id != snapshot.userId || journal.epoch != snapshot.epoch || journal.hasGapAfter(snapshot.revision)) {
            return images.toSet()
        }
        val owners =
            journal
                .changesAfter(snapshot.revision)
                .mapNotNull {
                    when (val change = it.change) {
                        is ReviewChange.AccessChanged -> change.userId
                        is ReviewChange.Written -> change.userId
                        else -> null
                    }
                }.toSet()
        return images.filterTo(mutableSetOf()) { image -> owners.any { image.path.startsWith("$it/") } }
    }
}

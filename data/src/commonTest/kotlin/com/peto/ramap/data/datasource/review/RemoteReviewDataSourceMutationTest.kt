package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.ReviewMutationResponse
import com.peto.ramap.data.model.ReviewOwnerCountsResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.fake.FakeReviewChangeStore
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(SupabaseInternal::class)
class RemoteReviewDataSourceMutationTest {
    @Test
    fun `same paths retain URLs while new and previously failed paths alone request signing`() =
        runTest {
            val calls = mutableListOf<List<SignedImageRequest>>()
            var rejectNew = true
            val signer =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
                        calls += images
                        return images.associateWith { request ->
                            if (request.path == "owner/new" && rejectNew) null else SignedImageUrl("url-${request.path}", Clock.System.now() + 5.minutes)
                        }
                    }
                }
            val client =
                createSupabaseClient("https://example.com", "test-key") {
                    httpEngine = MockEngine { error("Hydration must not query reviews") }
                    install(Storage)
                    install(Auth) {
                        minimalConfig()
                        autoSetupPlatform = false
                    }
                }
            client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
            try {
                val changes = FakeReviewChangeStore()
                val source = RemoteReviewDataSource(client, signer, changes)
                source.hydrateMutation(receipt(listOf("owner/retained")))
                val pending = receipt(listOf("owner/retained", "owner/new"))
                source.hydrateMutation(pending)
                rejectNew = false
                val hydrated = source.hydrateMutation(pending)
                assertEquals(listOf(listOf("owner/retained"), listOf("owner/new"), listOf("owner/new")), calls.map { requests -> requests.map { it.path } })
                assertEquals(listOf("url-owner/retained", "url-owner/new"), hydrated.after?.imageUrls)
                changes.publish(ReviewChange.AccessChanged("owner"))
                source.hydrateMutation(pending)
                assertEquals(listOf("owner/retained", "owner/new"), calls.last().map { it.path })
                changes.reset("another-viewer")
                source.hydrateMutation(pending)
                assertEquals(listOf("owner/retained", "owner/new"), calls.last().map { it.path })
            } finally {
                client.close()
            }
        }

    private fun receipt(paths: List<String>) =
        ReviewMutationResponse(
            reviewId = "review",
            userId = "owner",
            shopId = "shop",
            after = ReviewResponse("review", "shop", "리뷰 본문", "2026-10-12", imagePaths = paths, userId = "owner"),
            counts = ReviewOwnerCountsResponse(1, 1, 0, 1, true),
        )
}

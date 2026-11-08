package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.datasource.image.CachedImageUrlDataSource
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.ReviewMutationResponse
import com.peto.ramap.data.model.ReviewOwnerCountsResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.network.client.media.MediaStorageClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(SupabaseInternal::class)
class RemoteReviewDataSourceMutationTest {
    @Test
    fun `같은 경로의 URL은 유지하고 새 경로와 발급에 실패한 경로만 서명한다`() =
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
                    install(Auth) {
                        minimalConfig()
                        autoSetupPlatform = false
                    }
                }
            client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
            val mediaHttp = HttpClient(MockEngine { error("예상하지 않은 사진 API") })
            try {
                val changes = FakeReviewChangeStore()
                val images = CachedImageUrlDataSource(signer, ReviewImageUrlAccessPolicy(client, lazy { changes }))
                val source = RemoteReviewDataSource(client, ReviewImageResolver(images), MediaStorageClient(client, mediaHttp))
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
                mediaHttp.close()
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

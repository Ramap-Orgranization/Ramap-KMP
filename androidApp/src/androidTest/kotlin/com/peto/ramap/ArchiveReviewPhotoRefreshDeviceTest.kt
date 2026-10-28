package com.peto.ramap

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import com.peto.ramap.domain.model.review.change.ReviewSnapshot
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ArchiveRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.lifecycle.CompositionViewModelStoreOwner
import com.peto.ramap.ui.main.map.archive.ArchiveMapViewModel
import com.peto.ramap.ui.main.map.archive.ArchiveShopDetailContent
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapIntent
import com.peto.ramap.ui.main.map.archive.model.ArchiveShopDetailUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class ArchiveReviewPhotoRefreshDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `삭제_후_남은_사진의_자동_갱신과_다운로드가_오류_안내_없이_완료된다`() = verifyAutomaticRefresh(failRefresh = false)

    @Test
    fun `삭제_후_남은_사진의_자동_갱신이_실패했을_때만_재시도_안내를_표시한다`() = verifyAutomaticRefresh(failRefresh = true)

    private fun verifyAutomaticRefresh(failRefresh: Boolean) {
        val imageFile = File.createTempFile("archive-photo-refresh-", ".png", compose.activity.cacheDir)
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        imageFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val shop = ramenShopFixture(id = "photo-refresh-shop")
        val expiredPhoto = MyReviewPhoto("file://${imageFile.path}.expired", "remaining-photo", Clock.System.now() - 1.minutes)
        val remaining =
            MyReview(
                review = Review("remaining-review", shop.id, "남겨 둘 테스트 기록", "2026-10-26", images = listOf(expiredPhoto), isPublic = false),
                shopName = shop.name,
                shop = shop,
            )
        val removed =
            MyReview(
                review = Review("removed-review", shop.id, "삭제할 테스트 기록", "2026-10-27", isPublic = false),
                shopName = shop.name,
                shop = shop,
            )
        val shops = RamenShops(listOf(shop))
        val pending = CompletableDeferred<RamapResult<Map<String, MyReviewPhoto>>>()
        val requests = AtomicInteger()
        val downloaded = AtomicInteger()
        val sawError = AtomicBoolean()
        val changes = FakeReviewChangeStore()
        val repository =
            object : ArchiveRepository {
                override suspend fun fetchArchiveShops() = RamapResult.Success(ArchiveShops(listOf(removed, remaining), shops))

                override suspend fun fetchShops(shopIds: Set<String>) = RamapResult.Success(shops.filterByShopIds(shopIds))

                override suspend fun resolvePhotos(references: Set<String>): RamapResult<Map<String, MyReviewPhoto>> {
                    requests.incrementAndGet()
                    return pending.await()
                }
            }
        val owner = CompositionViewModelStoreOwner()
        lateinit var viewModel: ArchiveMapViewModel
        try {
            compose.runOnIdle {
                val factory = viewModelFactory { initializer { ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository()) } }
                viewModel = ViewModelProvider.create(owner, factory)[ArchiveMapViewModel::class]
                viewModel.viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.uiState.collect { if (it.hasReviewPhotoError) sawError.set(true) }
                }
                viewModel.dispatch(ArchiveMapIntent.Enter)
            }
            compose.waitUntil { viewModel.uiState.value.hasLoaded }
            compose.runOnIdle { viewModel.dispatch(ArchiveMapIntent.SelectShop(shop.id)) }
            compose.waitUntil { viewModel.uiState.value.selectedShopId == shop.id }
            compose.runOnIdle {
                changes.publish(
                    ReviewChange.Written(
                        reviewId = removed.review.id,
                        userId = "owner",
                        shopId = shop.id,
                        before = ReviewSnapshot(removed.review, shop.name, shop = shop),
                        after = null,
                        counts = ReviewOwnerCounts(1, 0, 1, 0, false),
                    ),
                )
            }
            compose.waitUntil { viewModel.uiState.value.selectedReviews.size == 1 }
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    RamapTheme {
                        ArchiveShopDetailContent(
                            request =
                                ArchiveShopDetailUiModel(
                                    shop = shop,
                                    reviews = state.selectedReviews,
                                    isReviewPhotosLoading = state.isReviewPhotosLoading,
                                    hasReviewPhotoError = state.hasReviewPhotoError,
                                    reviewPhotoRetryKeys = state.reviewPhotoRetryKeys,
                                    onRetryReviewPhotos = { viewModel.dispatch(ArchiveMapIntent.RetryReviewPhotos) },
                                    onReviewPhotoDownloadFailed = { viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed(it)) },
                                    onReviewPhotoDownloadSucceeded = {
                                        downloaded.incrementAndGet()
                                        viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded(it))
                                    },
                                    deleteTargetReviewId = null,
                                    isDeletingReview = false,
                                    onRequestReviewDeletion = {},
                                    onDismissReviewDeletion = {},
                                    onConfirmReviewDeletion = {},
                                    onDismiss = {},
                                ),
                            onEditReview = { _, _ -> },
                            sheetContent = { state, _, content -> LazyColumn(modifier = Modifier.fillMaxSize(), state = state, content = content) },
                            shopInformationContent = {},
                        )
                    }
                }
            }
            compose.waitUntil(timeoutMillis = 10_000) { requests.get() == 1 && viewModel.uiState.value.isReviewPhotosLoading }
            compose.onNodeWithText("다시 시도").assertDoesNotExist()
            assertFalse(sawError.get(), "자동 갱신 중에는 재시도 안내가 나타나면 안 됩니다")
            compose.onNodeWithText(remaining.review.body).assertIsDisplayed()
            compose.onNodeWithText(removed.review.body).assertDoesNotExist()
            if (failRefresh) {
                pending.complete(RamapResult.Error(RamapError.Http(500)))
                compose.waitUntil { viewModel.uiState.value.hasReviewPhotoError }
                compose.onNodeWithText("다시 시도").assertIsDisplayed()
                assertTrue(sawError.get())
            } else {
                pending.complete(RamapResult.Success(mapOf(expiredPhoto.reference to expiredPhoto.copy(url = imageFile.toURI().toString(), expiresAt = Clock.System.now() + 5.minutes))))
                compose.waitUntil(timeoutMillis = 10_000) { downloaded.get() > 0 && !viewModel.uiState.value.isReviewPhotosLoading }
                compose.onNodeWithText("다시 시도").assertDoesNotExist()
                compose.onNodeWithContentDescription("리뷰 사진 1").assertIsDisplayed()
                assertFalse(sawError.get(), "사진 다운로드 완료까지 재시도 안내가 깜빡이면 안 됩니다")
            }
            assertEquals(1, requests.get())
        } finally {
            compose.runOnIdle { owner.viewModelStore.clear() }
            imageFile.delete()
        }
    }
}

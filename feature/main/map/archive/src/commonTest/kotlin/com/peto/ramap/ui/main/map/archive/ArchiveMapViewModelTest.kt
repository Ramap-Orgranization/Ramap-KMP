package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import com.peto.ramap.domain.model.review.change.ReviewSnapshot
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapIntent
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapUiState
import com.peto.ramap.ui.main.map.archive.model.CameraPosition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveMapViewModelTest {
    @Test
    fun `빈 아카이브를 떠나 사진 없이 첫 리뷰를 작성하면 복귀 시 매장 기록을 연다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits())
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            viewModel.dispatch(ArchiveMapIntent.Exit)
            runCurrent()
            val created = row("first", "unused").let { it.copy(review = it.review.copy(images = emptyList())) }

            changes.publish(written(null, created))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()

            assertEquals("shop", viewModel.uiState.value.selectedShopId)
            assertEquals(
                listOf("first"),
                viewModel.uiState.value.selectedReviews
                    .map { it.review.id },
            )
            assertTrue(viewModel.uiState.value.shouldFocusVisits)
            assertTrue(
                viewModel.uiState.value.photos
                    .isEmpty(),
            )
            assertNull(viewModel.uiState.value.pendingShopSelection)
            assertEquals(1, repository.requests)
        }

    @Test
    fun `첫 매장 표시부터 사진 존재 정보를 포함하고 서명 대기를 사진 없음으로 노출하지 않는다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            val repository = FakeArchiveRepository(visits(row("review", "image"))).apply { pendingPhoto = pending }
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            val snapshots = mutableListOf<ArchiveMapUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { snapshots += it }
            }
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val pendingStates = snapshots.filter { it.hasLoaded && "shop" in it.shops }
            pending.complete(RamapResult.Success(MyReviewPhoto("resolved", "image")))
            runCurrent()

            assertTrue(pendingStates.isNotEmpty())
            assertTrue(pendingStates.all { it.photos["shop"]?.reference == "image" })
            assertTrue(pendingStates.all { it.photos["shop"]?.url == null })
            assertTrue(pendingStates.all { it.shouldFocusVisits })
            assertEquals(
                "resolved",
                viewModel.uiState.value.photos["shop"]
                    ?.url,
            )
        }

    @Test
    fun `대표 사진 서명 실패는 마커 대체 상태를 표시하고 상세 재시도 성공 시 복구한다`() =
        coroutinesTest {
            val failures: List<RamapResult<MyReviewPhoto>> =
                listOf(
                    RamapResult.Error(RamapError.Http(500)),
                    RamapResult.Success(MyReviewPhoto(null, "image")),
                )
            for (failure in failures) {
                val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
                val repository = FakeArchiveRepository(visits(row("review", "image"))).apply { pendingPhoto = pending }
                val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
                viewModel.dispatch(ArchiveMapIntent.Enter)
                runCurrent()
                assertTrue(
                    viewModel.uiState.value.photoState.failedMarkerSignatures
                        .isEmpty(),
                )

                pending.complete(failure)
                runCurrent()
                assertEquals(setOf("image"), viewModel.uiState.value.photoState.failedMarkerSignatures)
                assertTrue("shop" in viewModel.uiState.value.shops)

                val detailFailure = CompletableDeferred<RamapResult<MyReviewPhoto>>()
                repository.pendingPhoto = detailFailure
                viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
                runCurrent()
                detailFailure.complete(failure)
                runCurrent()
                assertTrue(viewModel.uiState.value.hasReviewPhotoError)

                viewModel.dispatch(ArchiveMapIntent.RetryReviewPhotos)
                runCurrent()
                assertFalse(viewModel.uiState.value.hasReviewPhotoError)
                assertTrue(
                    viewModel.uiState.value.photoState.failedMarkerSignatures
                        .isEmpty(),
                )
                assertEquals(
                    "https://example.com/image",
                    viewModel.uiState.value.photos["shop"]
                        ?.url,
                )
            }
        }

    @Test
    fun `여러 매장의 첫 대표 사진만 한 번의 batch로 서명한다`() =
        coroutinesTest {
            val first = row("first", "first-image").let { it.copy(review = it.review.copy(images = it.review.images + MyReviewPhoto(null, "unused-second"))) }
            val second = row("second", "second-image").let { it.copy(review = it.review.copy(shopId = "second-shop")) }
            val repository =
                FakeArchiveRepository(
                    ArchiveShops(listOf(first, second), RamenShops(listOf(ramenShopFixture(id = "shop"), ramenShopFixture(id = "second-shop")))),
                )
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            assertEquals(listOf(setOf("first-image", "second-image")), repository.photoBatches)
        }

    @Test
    fun `방문 모드 왕복과 상세 닫기는 카메라를 유지하며 조회와 서명을 반복하지 않는다`() =
        coroutinesTest {
            val repository = FakeArchiveRepository(visits(row("review", "first")))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val camera = CameraPosition(Location(36.2, 128.1), 14.0)
            viewModel.dispatch(ArchiveMapIntent.FocusConsumed)
            viewModel.dispatch(ArchiveMapIntent.CameraChanged(camera))
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            viewModel.dispatch(ArchiveMapIntent.SelectShop(null))
            viewModel.dispatch(ArchiveMapIntent.Exit)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            assertEquals(camera, viewModel.uiState.value.cameraPosition)
            assertFalse(viewModel.uiState.value.shouldFocusVisits)
            assertEquals(1, repository.requests)
            assertEquals(listOf("first"), repository.photoRequests)
        }

    @Test
    fun `대표 리뷰 삭제는 과거 사진으로 바꾸고 마지막 방문 삭제는 매장을 제거한다`() =
        coroutinesTest {
            val newest = row("new", "new", "2026-10-26")
            val previous = row("old", "old", "2026-10-25")
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits(newest, previous))
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            changes.publish(written(newest, null))
            runCurrent()
            assertEquals(
                "old",
                viewModel.uiState.value.photos["shop"]
                    ?.reference,
            )
            changes.publish(written(previous, null))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            assertEquals(1, repository.requests)
        }

    @Test
    fun `작성자 ID가 없는 본인 스냅샷에 새 비공개 방문과 사진 보완을 반영한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits())
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val created = row("new", "first").copy(shop = ramenShopFixture(id = "shop"))
            changes.publish(written(null, created))
            runCurrent()
            assertEquals(setOf("shop"), viewModel.uiState.value.shops.keys)
            assertEquals(
                "first",
                viewModel.uiState.value.photos["shop"]
                    ?.reference,
            )
            changes.publish(ReviewChange.Hydrated(snapshot(created)))
            runCurrent()
            assertEquals(1, repository.requests)
        }

    @Test
    fun `초기 페이지 조회 중 리뷰 변경은 전체 스냅샷을 다시 읽어 offset 누락을 복구한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits(row("remaining", "image")))
            val pending = CompletableDeferred<RamapResult<ArchiveShops>>()
            repository.pending = pending
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            changes.publish(written(row("removed", "removed"), null))
            runCurrent()
            pending.complete(RamapResult.Success(visits(row("removed", "removed"))))
            runCurrent()
            assertEquals(2, repository.requests)
            assertEquals(
                "image",
                viewModel.uiState.value.photos["shop"]
                    ?.reference,
            )
        }

    @Test
    fun `계정 변경 후 늦은 방문 조회와 사진 응답은 이전 기록을 복원하지 않는다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits(row("old", "old")))
            val pending = CompletableDeferred<RamapResult<ArchiveShops>>()
            repository.pending = pending
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            changes.reset(null)
            runCurrent()
            pending.complete(RamapResult.Success(repository.visits))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.photos
                    .isEmpty(),
            )
            assertNull(viewModel.uiState.value.cameraPosition)
            assertEquals(0, repository.photoRequests.size)
        }

    @Test
    fun `대표 변경 뒤 이전 사진 응답은 새 대표를 덮지 않고 이전 대표로 돌아오면 재사용한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits(row("old", "old")))
            val newest = row("new", "new", "2026-10-27")
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            changes.publish(written(null, newest))
            runCurrent()
            pending.complete(RamapResult.Success(MyReviewPhoto("old-url", "old")))
            runCurrent()
            assertEquals(
                "new",
                viewModel.uiState.value.photos["shop"]
                    ?.reference,
            )
            changes.publish(written(newest, null))
            runCurrent()
            assertEquals(
                "old",
                viewModel.uiState.value.photos["shop"]
                    ?.reference,
            )
            assertEquals(
                "old-url",
                viewModel.uiState.value.photos["shop"]
                    ?.url,
            )
            assertEquals(listOf("old", "new"), repository.photoRequests)
        }

    @Test
    fun `대표 사진 서명 실패 뒤 재진입하면 방문을 재조회하지 않고 사진만 다시 요청한다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            val repository = FakeArchiveRepository(visits(row("review", "image"))).apply { pendingPhoto = pending }
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            pending.complete(RamapResult.Error(RamapError.Http(500)))
            runCurrent()
            assertNull(
                viewModel.uiState.value.photos["shop"]
                    ?.url,
            )
            viewModel.dispatch(ArchiveMapIntent.Exit)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            assertEquals(
                "https://example.com/image",
                viewModel.uiState.value.photos["shop"]
                    ?.url,
            )
            assertEquals(listOf("image", "image"), repository.photoRequests)
            assertEquals(1, repository.requests)
        }

    @Test
    fun `로그아웃 후 같은 계정으로 재진입하면 이전 방문 응답을 버리고 새 조회 결과를 유지한다`() =
        coroutinesTest {
            val oldVisits = visits(row("old", "old"))
            val pending = CompletableDeferred<RamapResult<ArchiveShops>>()
            val repository = FakeArchiveRepository(oldVisits).apply { this.pending = pending }
            val changes = FakeReviewChangeStore()
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            changes.reset(null)
            runCurrent()
            repository.visits = visits(row("new", "new"))
            changes.reset("owner")
            runCurrent()
            assertEquals(
                "new",
                viewModel.uiState.value.photos["shop"]
                    ?.reference,
            )
            pending.complete(RamapResult.Success(oldVisits))
            runCurrent()
            assertEquals("owner", viewModel.uiState.value.userId)
            assertEquals(
                "new",
                viewModel.uiState.value.photos["shop"]
                    ?.reference,
            )
            assertEquals(
                "https://example.com/new",
                viewModel.uiState.value.photos["shop"]
                    ?.url,
            )
            assertEquals(2, repository.requests)
            assertEquals(listOf("new"), repository.photoRequests)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `본인 공개범위 변경은 방문을 삭제하지 않고 스냅샷을 다시 확인한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits(row("visit", "image")))
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val updated = row("visit", "image")
            changes.publish(written(updated, updated))
            runCurrent()
            changes.publish(ReviewChange.AccessChanged("owner"))
            runCurrent()
            assertEquals(setOf("shop"), viewModel.uiState.value.shops.keys)
            assertEquals(2, repository.requests)
            changes.publish(ReviewChange.AccessChanged("other"))
            runCurrent()
            assertEquals(2, repository.requests)
            assertEquals(setOf("shop"), viewModel.uiState.value.shops.keys)
        }

    @Test
    fun `만료된 사진 다운로드 실패는 한 번만 서명을 갱신한다`() =
        coroutinesTest {
            val expired = row("review", "image").let { it.copy(review = it.review.copy(images = listOf(MyReviewPhoto("expired", "image", Clock.System.now() - 1.minutes)))) }
            val repository = FakeArchiveRepository(visits(expired))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            repeat(3) { viewModel.dispatch(ArchiveMapIntent.PhotoDownloadFailed("shop", "image")) }
            runCurrent()
            assertEquals(listOf("image"), repository.photoRequests)
        }

    @Test
    fun `선택한 매장만 작성자 ID 없이 최신순으로 모든 사진을 표시한다`() =
        coroutinesTest {
            val newest = row("b", "first").let { it.copy(review = it.review.copy(images = it.review.images + MyReviewPhoto(null, "second"))) }
            val previous = row("a", "older")
            val other = row("other", "other-first").let { it.copy(review = it.review.copy(shopId = "other-shop", images = it.review.images + MyReviewPhoto(null, "other-unused"))) }
            val repository = FakeArchiveRepository(ArchiveShops(listOf(previous, other, newest), RamenShops(listOf(ramenShopFixture(id = "shop"), ramenShopFixture(id = "other-shop")))))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            assertEquals(
                listOf("b", "a"),
                viewModel.uiState.value.selectedReviews
                    .map { it.review.id },
            )
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .flatMap { it.review.images }
                    .all { it.url != null },
            )
            assertEquals(listOf(setOf("first", "other-first"), setOf("second", "older")), repository.photoBatches)
            assertEquals(1, repository.requests)
        }

    @Test
    fun `진행 중인 대표 사진 서명을 기다리고 상세에서 중복 서명하지 않는다`() =
        coroutinesTest {
            val repository = FakeArchiveRepository(visits(row("review", "image")))
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            assertTrue(viewModel.uiState.value.isReviewPhotosLoading)
            pending.complete(RamapResult.Success(MyReviewPhoto("resolved", "image")))
            runCurrent()
            assertEquals(listOf(setOf("image")), repository.photoBatches)
            assertEquals(
                "resolved",
                viewModel.uiState.value.selectedReviews
                    .single()
                    .review.images
                    .single()
                    .url,
            )
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
        }

    @Test
    fun `상세 사진 응답 전에 리뷰를 삭제하면 삭제한 기록을 복원하지 않는다`() =
        coroutinesTest {
            val newest = row("new", "new", "2026-10-27")
            val previous = row("old", "old")
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits(newest, previous))
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            changes.publish(written(previous, null))
            runCurrent()
            pending.complete(RamapResult.Success(MyReviewPhoto("removed", "old")))
            runCurrent()
            assertEquals(
                listOf("new"),
                viewModel.uiState.value.selectedReviews
                    .map { it.review.id },
            )
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
        }

    @Test
    fun `매장 선택 변경과 닫기는 늦은 사진 응답에서 이전 기록을 복원하지 않는다`() =
        coroutinesTest {
            val first = row("first", "image").let { it.copy(review = it.review.copy(images = it.review.images + MyReviewPhoto(null, "pending"))) }
            val other = row("other", "other-image").let { it.copy(review = it.review.copy(shopId = "other-shop")) }
            val repository = FakeArchiveRepository(ArchiveShops(listOf(first, other), RamenShops(listOf(ramenShopFixture(id = "shop"), ramenShopFixture(id = "other-shop")))))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("other-shop"))
            runCurrent()
            assertEquals(
                listOf("other"),
                viewModel.uiState.value.selectedReviews
                    .map { it.review.id },
            )
            viewModel.dispatch(ArchiveMapIntent.Exit)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .isEmpty(),
            )
            pending.complete(RamapResult.Success(MyReviewPhoto("old", "pending")))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
        }

    @Test
    fun `계정 변경은 선택 기록과 로딩을 즉시 비우고 늦은 사진 응답을 버린다`() =
        coroutinesTest {
            val review = row("review", "first").let { it.copy(review = it.review.copy(images = it.review.images + MyReviewPhoto(null, "pending"))) }
            val repository = FakeArchiveRepository(visits(review))
            val changes = FakeReviewChangeStore()
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            changes.reset(null)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
            pending.complete(RamapResult.Success(MyReviewPhoto("old", "pending")))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .isEmpty(),
            )
        }

    @Test
    fun `같은 매장을 닫고 다시 열면 취소된 사진 응답은 새 요청의 상태를 변경하지 않는다`() =
        coroutinesTest {
            val review = row("review", "first").let { it.copy(review = it.review.copy(images = it.review.images + MyReviewPhoto(null, "pending"))) }
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val oldResponse = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = oldResponse
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop(null))
            runCurrent()
            val newResponse = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = newResponse
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            oldResponse.complete(RamapResult.Success(MyReviewPhoto("stale-url", "pending")))
            runCurrent()
            assertTrue(viewModel.uiState.value.isReviewPhotosLoading)
            assertNull(
                viewModel.uiState.value.selectedReviews
                    .single()
                    .review.images
                    .last()
                    .url,
            )
            newResponse.complete(RamapResult.Success(MyReviewPhoto("current-url", "pending")))
            runCurrent()
            assertEquals(
                "current-url",
                viewModel.uiState.value.selectedReviews
                    .single()
                    .review.images
                    .last()
                    .url,
            )
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
        }

    @Test
    fun `상세 사진 서명 실패는 기록을 유지하고 재시도로 복구한다`() =
        coroutinesTest {
            val review = row("review", "first").let { it.copy(review = it.review.copy(images = it.review.images + MyReviewPhoto(null, "retry"))) }
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            pending.complete(RamapResult.Error(RamapError.Http(500)))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasReviewPhotoError)
            assertEquals(1, viewModel.uiState.value.selectedReviews.size)
            viewModel.dispatch(ArchiveMapIntent.RetryReviewPhotos)
            runCurrent()
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .single()
                    .review.images
                    .all { it.url != null },
            )
        }

    @Test
    fun `리뷰 수정은 타임라인 본문과 사진을 함께 교체하고 이전 응답을 버린다`() =
        coroutinesTest {
            val original = row("review", "first").let { it.copy(review = it.review.copy(images = it.review.images + MyReviewPhoto(null, "removed"))) }
            val repository = FakeArchiveRepository(visits(original))
            val changes = FakeReviewChangeStore()
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            val updated = original.copy(review = original.review.copy(body = "수정한 방문 후기입니다", images = listOf(MyReviewPhoto(null, "replacement"))))
            changes.publish(written(original, updated))
            runCurrent()
            pending.complete(RamapResult.Success(MyReviewPhoto("removed", "removed")))
            runCurrent()
            val selected =
                viewModel.uiState.value.selectedReviews
                    .single()
                    .review
            assertEquals("수정한 방문 후기입니다", selected.body)
            assertEquals(listOf("replacement"), selected.images.map { it.reference })
            assertTrue(selected.images.all { it.url != null })
        }

    @Test
    fun `상세 만료 사진은 자동 갱신 한 번 이후 사용자 재시도로 다시 갱신한다`() =
        coroutinesTest {
            val expiredPhoto = MyReviewPhoto("expired", "image", Clock.System.now() - 1.minutes)
            val review = row("review", "image").let { it.copy(review = it.review.copy(images = listOf(expiredPhoto))) }
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            repeat(3) { viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed("image")) }
            runCurrent()
            assertEquals(listOf("image"), repository.photoRequests)
            pending.complete(RamapResult.Success(expiredPhoto))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed("image"))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasReviewPhotoError)
            assertEquals(listOf("image"), repository.photoRequests)
            viewModel.dispatch(ArchiveMapIntent.RetryReviewPhotos)
            runCurrent()
            assertEquals(listOf("image", "image"), repository.photoRequests)
            assertTrue(viewModel.uiState.value.hasReviewPhotoError)
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded("image"))
            runCurrent()
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
        }

    @Test
    fun `리뷰 삭제 후 남은 만료 사진을 자동 갱신하는 동안 재시도 오류를 표시하지 않는다`() =
        coroutinesTest {
            val newest = row("new", "unused", "2026-10-27").let { it.copy(review = it.review.copy(images = emptyList())) }
            val expired = MyReviewPhoto("expired", "image", Clock.System.now() - 1.minutes)
            val previous = row("old", "image").let { it.copy(review = it.review.copy(images = listOf(expired))) }
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits(newest, previous))
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            changes.publish(written(newest, null))
            runCurrent()
            val observed = mutableListOf<ArchiveMapUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { observed += it } }
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            repeat(3) { viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed("image")) }
            runCurrent()
            assertTrue(viewModel.uiState.value.isReviewPhotosLoading)
            assertEquals(listOf("image"), repository.photoRequests)
            val refreshed = expired.copy(expiresAt = Clock.System.now() + 5.minutes)
            pending.complete(RamapResult.Success(refreshed))
            runCurrent()
            assertEquals(1, viewModel.uiState.value.reviewPhotoRetryKeys["image"])
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded("image"))
            runCurrent()
            assertTrue(observed.none { it.hasReviewPhotoError }, "자동 갱신 전후 어느 상태에서도 오류 안내가 깜빡이면 안 됩니다")
            assertEquals(
                listOf("old"),
                viewModel.uiState.value.selectedReviews
                    .map { it.review.id },
            )
        }

    @Test
    fun `만료 사진 자동 갱신이 실패하면 오류를 표시하고 사용자 재시도로 복구한다`() =
        coroutinesTest {
            val photo = MyReviewPhoto("expired", "image", Clock.System.now() - 1.minutes)
            val review = row("review", "image").let { it.copy(review = it.review.copy(images = listOf(photo))) }
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed("image"))
            runCurrent()
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
            pending.complete(RamapResult.Error(RamapError.Http(500)))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasReviewPhotoError)
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
            viewModel.dispatch(ArchiveMapIntent.RetryReviewPhotos)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded("image"))
            runCurrent()
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
        }

    @Test
    fun `자동 갱신 응답에서 사진 URL이 누락되면 만료 URL이 남아 있어도 오류를 표시한다`() =
        coroutinesTest {
            val photo = MyReviewPhoto("expired", "image", Clock.System.now() - 1.minutes)
            val review = row("review", "image").let { it.copy(review = it.review.copy(images = listOf(photo))) }
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed("image"))
            runCurrent()
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
            pending.complete(RamapResult.Success(MyReviewPhoto(null, "image")))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasReviewPhotoError)
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
        }

    @Test
    fun `자동 갱신 중인 리뷰가 삭제되면 갱신 상태와 늦은 실패 응답을 버린다`() =
        coroutinesTest {
            val photo = MyReviewPhoto("expired", "image", Clock.System.now() - 1.minutes)
            val review = row("review", "image").let { it.copy(review = it.review.copy(images = listOf(photo))) }
            val changes = FakeReviewChangeStore()
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed("image"))
            runCurrent()
            assertTrue(viewModel.uiState.value.isReviewPhotosLoading)
            changes.publish(written(review, null))
            runCurrent()
            pending.complete(RamapResult.Error(RamapError.Http(500)))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.photoState.refreshingReviewDownloads
                    .isEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
        }

    @Test
    fun `동일 URL 재시도도 사진 키를 증가시키며 실제 다운로드 성공까지 오류를 유지한다`() =
        coroutinesTest {
            val photo = MyReviewPhoto("https://example.com/image", "image", Clock.System.now() + 5.minutes)
            val review = row("review", "image").let { it.copy(review = it.review.copy(images = listOf(photo))) }
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed("image"))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasReviewPhotoError)
            repeat(2) { attempt ->
                val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
                repository.pendingPhoto = pending
                viewModel.dispatch(ArchiveMapIntent.RetryReviewPhotos)
                runCurrent()
                assertTrue(viewModel.uiState.value.hasReviewPhotoError)
                assertTrue(viewModel.uiState.value.isReviewPhotosLoading)
                pending.complete(RamapResult.Success(photo))
                runCurrent()
                assertEquals(attempt + 1, viewModel.uiState.value.reviewPhotoRetryKeys["image"])
                assertEquals(
                    photo.url,
                    viewModel.uiState.value.selectedReviews
                        .single()
                        .review.images
                        .single()
                        .url,
                )
                assertTrue(viewModel.uiState.value.hasReviewPhotoError)
            }
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded("other"))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasReviewPhotoError)
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded("image"))
            runCurrent()
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
            viewModel.dispatch(ArchiveMapIntent.SelectShop(null))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.reviewPhotoRetryKeys
                    .isEmpty(),
            )
        }

    @Test
    fun `선택을 닫은 뒤 늦은 재시도 응답과 성공 알림은 사진 키나 기록을 복원하지 않는다`() =
        coroutinesTest {
            val photo = MyReviewPhoto("https://example.com/image", "image", Clock.System.now() + 5.minutes)
            val review = row("review", "image").let { it.copy(review = it.review.copy(images = listOf(photo))) }
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed("image"))
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.RetryReviewPhotos)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop(null))
            runCurrent()
            pending.complete(RamapResult.Success(photo))
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded("image"))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .isEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.reviewPhotoRetryKeys
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
        }

    @Test
    fun `대표 사진 다운로드 성공은 추가 사진 서명 대기 상태를 오류로 표시하지 않는다`() =
        coroutinesTest {
            val review = row("review", "first").let { it.copy(review = it.review.copy(images = it.review.images + MyReviewPhoto(null, "pending"))) }
            val repository = FakeArchiveRepository(visits(review))
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            repository.pendingPhoto = pending
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            assertTrue(viewModel.uiState.value.isReviewPhotosLoading)
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
            viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded("first"))
            runCurrent()
            assertTrue(viewModel.uiState.value.isReviewPhotosLoading)
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
            pending.complete(RamapResult.Success(MyReviewPhoto("resolved", "pending")))
            runCurrent()
            assertFalse(viewModel.uiState.value.isReviewPhotosLoading)
            assertFalse(viewModel.uiState.value.hasReviewPhotoError)
        }

    @Test
    fun `최초 조회 실패 뒤 재진입은 오류를 유지하고 명시적 재시도로 복구한다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<ArchiveShops>>()
            val repository = FakeArchiveRepository(visits(row("review", "image"))).apply { this.pending = pending }
            val viewModel = ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            pending.complete(RamapResult.Error(RamapError.Http(500)))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.Exit)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            assertEquals(1, repository.requests)
            assertTrue(viewModel.uiState.value.hasError)
            assertFalse(viewModel.uiState.value.hasLoaded)
            viewModel.dispatch(ArchiveMapIntent.Retry)
            runCurrent()
            assertEquals(2, repository.requests)
            assertFalse(viewModel.uiState.value.hasError)
            assertTrue(viewModel.uiState.value.hasLoaded)
        }

    @Test
    fun `같은 변경 묶음에서 마지막 리뷰를 교체해도 선택한 매장을 닫지 않는다`() =
        coroutinesTest {
            val original = row("original", "original")
            val replacement = row("replacement", "replacement", "2026-10-27")
            val repository = FakeArchiveRepository(visits(original))
            val changes = FakeReviewChangeStore()
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("original"))
            runCurrent()
            val snapshots = mutableListOf<ArchiveMapUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { snapshots += it }
            }
            changes.publish(written(original, null))
            changes.publish(written(null, replacement))
            runCurrent()
            assertTrue(snapshots.all { it.selectedShopId == "shop" })
            assertEquals(
                listOf("replacement"),
                viewModel.uiState.value.selectedReviews
                    .map { it.review.id },
            )
            assertEquals(
                "replacement",
                viewModel.uiState.value.photos["shop"]
                    ?.reference,
            )
            assertNull(viewModel.uiState.value.deleteTargetReviewId)
            assertEquals(1, repository.requests)
        }

    @Test
    fun `대표 사진 대기 중 리뷰 변경은 중복 서명하지 않고 이전 상태를 변경하지 않는다`() =
        coroutinesTest {
            val original = row("review", "image")
            val pending = CompletableDeferred<RamapResult<MyReviewPhoto>>()
            val repository = FakeArchiveRepository(visits(original)).apply { pendingPhoto = pending }
            val changes = FakeReviewChangeStore()
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            val previousState = viewModel.uiState.value
            val updated = original.copy(review = original.review.copy(body = "수정한 방문 후기입니다"))
            changes.publish(written(original, updated))
            runCurrent()
            assertEquals(listOf(setOf("image")), repository.photoBatches)
            pending.complete(RamapResult.Success(MyReviewPhoto("resolved", "image")))
            runCurrent()
            assertTrue(previousState.photoState.resolved.isEmpty())
            assertNull(previousState.photos["shop"]?.url)
            assertEquals(original, previousState.visits.reviews.single())
            assertEquals(
                "resolved",
                viewModel.uiState.value.photos["shop"]
                    ?.url,
            )
            assertEquals(
                "수정한 방문 후기입니다",
                viewModel.uiState.value.visits.reviews
                    .single()
                    .review.body,
            )
        }

    @Test
    fun `화면 종료 뒤 계정 변경은 상태를 비우고 다시 진입할 때 조회한다`() =
        coroutinesTest {
            val repository = FakeArchiveRepository(visits(row("review", "image")))
            val changes = FakeReviewChangeStore()
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), FakeShopReviewRepository())
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.FocusConsumed)
            viewModel.dispatch(ArchiveMapIntent.Exit)
            runCurrent()
            changes.reset(null)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.visits.reviews
                    .isEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.photoState.resolved
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.hasFocusedVisits)
            assertFalse(viewModel.uiState.value.isEntered)
            changes.reset("owner")
            runCurrent()
            assertEquals(1, repository.requests)
            assertFalse(viewModel.uiState.value.hasLoaded)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            assertEquals(2, repository.requests)
            assertTrue(viewModel.uiState.value.hasLoaded)
            assertTrue(viewModel.uiState.value.isEntered)
        }

    private fun row(
        id: String,
        reference: String,
        date: String = "2026-10-26",
    ) = MyReview(
        review = Review(id, "shop", "방문 후기입니다", date, images = listOf(MyReviewPhoto(null, reference)), isPublic = false),
        shopName = "매장",
    )

    private fun visits(vararg rows: MyReview) = ArchiveShops(rows.toList(), RamenShops(listOf(ramenShopFixture(id = "shop"))))

    private fun snapshot(row: MyReview) = ReviewSnapshot(row.review, row.shopName, shop = row.shop)

    private fun written(
        before: MyReview?,
        after: MyReview?,
    ) = ReviewChange.Written(
        reviewId = requireNotNull(before ?: after).review.id,
        userId = "owner",
        shopId = "shop",
        before = before?.let(::snapshot),
        after = after?.let(::snapshot),
        counts = ReviewOwnerCounts(1, 0, 1, 0, false),
    )
}
